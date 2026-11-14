package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.BillingDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.InvoiceData;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.InvoiceRequest;
import com.littlehorizons.erp.model.PaymentItem;
import com.littlehorizons.erp.model.PaymentReceipt;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcBillingDao implements BillingDao {

    /** Invoicing resolution (DIAN) currently in use. */
    private record Resolution(int id, String number, LocalDate date, String prefix, int from, int to, int current) { }

    private record ConceptInfo(String code, BigDecimal vatRate) { }

    private record InvoiceHeader(int id, InvoiceData data) { }

    @Override
    public PaymentReceipt registerPayment(InvoiceRequest req, CufeCalculator cufeCalculator) {
        return Jdbc.inTransaction("Register payment", conn -> {
            if (req.items().isEmpty()) {
                throw new BusinessRuleException("La factura debe tener al menos un ítem.");
            }

            // 1. Invoice lines (amounts include VAT; subtotal and VAT are derived from the concept's rate).
            List<InvoiceLine> lines = new ArrayList<>();
            int position = 1;
            for (PaymentItem item : req.items()) {
                if (item.quantity() <= 0 || item.unitAmount().signum() <= 0) {
                    throw new BusinessRuleException("Cada ítem debe tener cantidad y valor mayores que cero.");
                }
                ConceptInfo info = Jdbc.queryOne(conn,
                        "SELECT codigo_concepto, porcentaje_iva FROM conceptos_pago WHERE id_concepto = ?",
                        args(item.conceptId()), rs -> new ConceptInfo(rs.getString(1), rs.getBigDecimal(2)))
                        .orElseThrow(() -> new BusinessRuleException("El concepto de pago no existe."));
                BigDecimal lineTotal = item.unitAmount().multiply(BigDecimal.valueOf(item.quantity()));
                lines.add(InvoiceLine.of(position++, info.code(), item.description(), item.quantity(), lineTotal,
                        info.vatRate()));
            }
            BigDecimal subtotal = lines.stream().map(InvoiceLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal vat = lines.stream().map(InvoiceLine::vat).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = subtotal.add(vat);

            // 2. Lock the active resolution and take the next consecutive number.
            Resolution res = Jdbc.queryOne(conn, """
                    SELECT id_resolucion, numero_resolucion, fecha_resolucion, prefijo, rango_desde, rango_hasta,
                           consecutivo_actual
                    FROM resoluciones_facturacion
                    WHERE esta_activa = TRUE AND fecha_vigencia_hasta >= ?
                    ORDER BY id_resolucion DESC LIMIT 1 FOR UPDATE""", args(req.paymentDate()),
                    rs -> new Resolution(rs.getInt("id_resolucion"), rs.getString("numero_resolucion"),
                            Jdbc.date(rs, "fecha_resolucion"), rs.getString("prefijo"), rs.getInt("rango_desde"),
                            rs.getInt("rango_hasta"), rs.getInt("consecutivo_actual")))
                    .orElseThrow(() -> new BusinessRuleException(
                            "No hay una resolución de facturación vigente. Configure una en la base de datos."));
            int next = res.current() + 1;
            if (next > res.to()) {
                throw new BusinessRuleException("Se agotó el rango de la resolución de facturación.");
            }
            Jdbc.update(conn, "UPDATE resoluciones_facturacion SET consecutivo_actual = ? WHERE id_resolucion = ?",
                    args(next, res.id()));
            String invoiceNumber = res.prefix() + "-" + String.format("%06d", next);
            String receiptNumber = InvoiceData.receiptNumber(req.paymentDate(), invoiceNumber);
            String cufe = cufeCalculator.compute(invoiceNumber, req.paymentDate(), total);

            // 3. Invoice header.
            int invoiceId = Jdbc.insert(conn, """
                    INSERT INTO facturas (numero_factura, id_resolucion, id_estudiante, id_acudiente, fecha_emision,
                      cufe, subtotal, valor_iva, total) VALUES (?,?,?,?,?,?,?,?,?)""",
                    args(invoiceNumber, res.id(), req.studentId(), req.guardianId(), req.paymentDate(), cufe,
                            subtotal, vat, total));

            // 4. One payment per item (and settle the charge it pays, if any).
            int index = 1;
            for (PaymentItem item : req.items()) {
                InvoiceLine line = lines.get(index - 1);
                int paymentId = Jdbc.insert(conn, """
                        INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, cantidad, monto_pagado,
                          fecha_pago, metodo_pago, numero_recibo, anio_academico, periodo_academico, id_factura)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?)""", args(req.studentId(), req.guardianId(), item.conceptId(),
                        item.quantity(), line.total(), req.paymentDate(), req.method(),
                        receiptNumber + String.format("-%02d", index), req.academicYear(), item.period(), invoiceId));
                if (item.chargeId() != null) {
                    String status = Jdbc.queryOne(conn, "SELECT estado FROM cargos WHERE id_cargo = ? "
                                    + "AND id_estudiante = ? AND id_concepto = ? FOR UPDATE",
                            args(item.chargeId(), req.studentId(), item.conceptId()), rs -> rs.getString(1))
                            .orElseThrow(() -> new BusinessRuleException("El cargo no existe para este estudiante."));
                    if (!"Pendiente".equals(status)) {
                        throw new BusinessRuleException("El cargo " + item.chargeId()
                                + " ya no está pendiente (estado: " + status + ").");
                    }
                    Jdbc.update(conn, "UPDATE cargos SET estado = 'Pagado', id_pago = ? WHERE id_cargo = ?",
                            args(paymentId, item.chargeId()));
                }
                index++;
            }

            String gradeName = Jdbc.queryOne(conn, """
                    SELECT g.nombre_grado FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado
                    WHERE m.id_estudiante = ? AND m.anio_academico = ? LIMIT 1""",
                    args(req.studentId(), req.academicYear()), rs -> rs.getString(1)).orElse("-");

            return new PaymentReceipt(invoiceId, invoiceNumber, receiptNumber, req.paymentDate(), cufe, subtotal, vat,
                    total, res.number(), res.date(), res.prefix(), res.from(), res.to(), gradeName, lines);
        });
    }

    @Override
    public void updateInvoiceFile(int invoiceId, String fileName, String path) {
        Jdbc.update("UPDATE facturas SET nombre_archivo = ?, ruta_archivo = ? WHERE id_factura = ?",
                args(fileName, path, invoiceId));
    }

    @Override
    public void updateInvoiceFileByNumber(String invoiceNumber, String fileName, String path) {
        Jdbc.update("UPDATE facturas SET nombre_archivo = ?, ruta_archivo = ? WHERE numero_factura = ?",
                args(fileName, path, invoiceNumber));
    }

    @Override
    public Optional<InvoiceData> findInvoiceData(String invoiceNumber) {
        String headerSql = """
                SELECT f.id_factura, f.numero_factura, f.fecha_emision, f.cufe, f.subtotal, f.valor_iva, f.total,
                       r.numero_resolucion, r.fecha_resolucion, r.prefijo, r.rango_desde, r.rango_hasta,
                       CONCAT_WS(' ', e.primer_nombre, e.primer_apellido, e.segundo_apellido) AS estudiante,
                       COALESCE(CONCAT_WS(' ', a.primer_nombre, a.segundo_nombre, a.primer_apellido,
                                a.segundo_apellido), '-') AS cliente,
                       COALESCE(CONCAT(a.tipo_documento, ' ', a.numero_documento), '-') AS documento_cliente,
                       COALESCE(a.direccion, '-') AS direccion_cliente,
                       COALESCE(a.telefono_principal, '-') AS telefono_cliente,
                       COALESCE(a.correo_electronico, '-') AS correo_cliente,
                       COALESCE((SELECT g.nombre_grado FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado
                                 WHERE m.id_estudiante = f.id_estudiante AND m.anio_academico = YEAR(f.fecha_emision)
                                 LIMIT 1), '-') AS grado,
                       COALESCE((SELECT p.metodo_pago FROM pagos p WHERE p.id_factura = f.id_factura
                                 ORDER BY p.id_pago LIMIT 1), '-') AS metodo
                FROM facturas f
                JOIN resoluciones_facturacion r ON r.id_resolucion = f.id_resolucion
                JOIN estudiantes e ON e.id_estudiante = f.id_estudiante
                LEFT JOIN acudientes a ON a.id_acudiente = f.id_acudiente
                WHERE f.numero_factura = ?""";
        String linesSql = """
                SELECT p.cantidad, p.monto_pagado, c.codigo_concepto, c.porcentaje_iva,
                       COALESCE(ca.descripcion, c.nombre_concepto) AS descripcion_item
                FROM pagos p
                JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                LEFT JOIN cargos ca ON ca.id_pago = p.id_pago
                WHERE p.id_factura = ? ORDER BY p.id_pago""";
        return Jdbc.withConnection("Find invoice " + invoiceNumber, conn -> {
            Optional<InvoiceHeader> header = Jdbc.queryOne(conn, headerSql, args(invoiceNumber), rs -> {
                LocalDate issued = Jdbc.date(rs, "fecha_emision");
                String number = rs.getString("numero_factura");
                InvoiceData data = new InvoiceData(number, issued, rs.getString("cufe"),
                        InvoiceData.resolutionText(rs.getString("numero_resolucion"),
                                Jdbc.date(rs, "fecha_resolucion"), rs.getString("prefijo"),
                                rs.getInt("rango_desde"), rs.getInt("rango_hasta")),
                        rs.getString("cliente"), rs.getString("documento_cliente"),
                        rs.getString("direccion_cliente"), rs.getString("telefono_cliente"),
                        rs.getString("correo_cliente"), rs.getString("estudiante"), rs.getString("grado"),
                        issued.getYear(), List.of(), rs.getBigDecimal("subtotal"), rs.getBigDecimal("valor_iva"),
                        rs.getBigDecimal("total"), rs.getString("metodo"),
                        InvoiceData.receiptNumber(issued, number));
                return new InvoiceHeader(rs.getInt("id_factura"), data);
            });
            if (header.isEmpty()) {
                return Optional.<InvoiceData>empty();
            }
            List<InvoiceLine> lines = new ArrayList<>();
            Jdbc.query(conn, linesSql, args(header.get().id()), rs -> lines.add(InvoiceLine.of(lines.size() + 1,
                    rs.getString("codigo_concepto"), rs.getString("descripcion_item"), rs.getInt("cantidad"),
                    rs.getBigDecimal("monto_pagado"), rs.getBigDecimal("porcentaje_iva"))));
            InvoiceData h = header.get().data();
            return Optional.of(new InvoiceData(h.invoiceNumber(), h.issueDate(), h.cufe(), h.resolutionText(),
                    h.customerName(), h.customerDocument(), h.customerAddress(), h.customerPhone(),
                    h.customerEmail(), h.studentName(), h.gradeName(), h.academicYear(), lines, h.subtotal(),
                    h.vat(), h.total(), h.paymentMethod(), h.receiptNumber()));
        });
    }

    @Override
    public ReportTable invoiceList() {
        return Jdbc.table("Facturas emitidas (las 50 más recientes)",
                List.of("Factura", "Fecha", "Estudiante", "Ítems", "Total", "Archivo"), """
                SELECT f.numero_factura, f.fecha_emision,
                       CONCAT_WS(' ', e.primer_nombre, e.primer_apellido, e.segundo_apellido),
                       (SELECT COUNT(*) FROM pagos p WHERE p.id_factura = f.id_factura),
                       FORMAT(f.total, 0), COALESCE(f.nombre_archivo, '(sin generar)')
                FROM facturas f JOIN estudiantes e ON e.id_estudiante = f.id_estudiante
                ORDER BY f.id_factura DESC LIMIT 50""", Jdbc.NO_ARGS);
    }

    @Override
    public ReportTable historyOfStudent(int studentId) {
        return Jdbc.table("Historial de pagos del estudiante", List.of("Fecha", "Recibo", "Concepto", "Cant.",
                "Monto", "Método", "Factura"), """
                SELECT p.fecha_pago, p.numero_recibo, c.nombre_concepto, p.cantidad, FORMAT(p.monto_pagado, 0),
                       p.metodo_pago, COALESCE(f.numero_factura, '-')
                FROM pagos p JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                LEFT JOIN facturas f ON f.id_factura = p.id_factura
                WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC""", args(studentId));
    }

    @Override
    public ReportTable historyOfGuardian(int guardianId) {
        return Jdbc.table("Historial de pagos del acudiente (todos sus estudiantes)", List.of("Fecha", "Estudiante",
                "Concepto", "Monto", "Recibo", "Factura"), """
                SELECT p.fecha_pago, CONCAT_WS(' ', e.primer_nombre, e.primer_apellido), c.nombre_concepto,
                       FORMAT(p.monto_pagado, 0), p.numero_recibo, COALESCE(f.numero_factura, '-')
                FROM pagos p
                JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                LEFT JOIN facturas f ON f.id_factura = p.id_factura
                WHERE p.id_estudiante IN (SELECT id_estudiante FROM estudiante_acudiente WHERE id_acudiente = ?)
                ORDER BY p.fecha_pago DESC, p.id_pago DESC""", args(guardianId));
    }

    @Override
    public ReportTable revenueByConcept(int year) {
        return Jdbc.table("Recaudo por concepto (" + year + ")", List.of("Concepto", "Pagos", "Total recaudado"), """
                SELECT c.nombre_concepto, COUNT(*), FORMAT(SUM(p.monto_pagado), 0)
                FROM pagos p JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                WHERE YEAR(p.fecha_pago) = ? GROUP BY c.id_concepto, c.nombre_concepto
                ORDER BY SUM(p.monto_pagado) DESC""", args(year));
    }
}
