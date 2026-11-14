package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.time.LocalDate;
import java.util.List;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcLoanDao implements LoanDao {

    @Override
    public int lend(BorrowerType type, int resourceId, int borrowerId, LocalDate loanDate, LocalDate dueDate) {
        return Jdbc.inTransaction("Lend resource", conn -> {
            String status = Jdbc.queryOne(conn, "SELECT estado_recurso FROM recursos WHERE id_recurso = ? FOR UPDATE",
                    args(resourceId), rs -> rs.getString(1))
                    .orElseThrow(() -> new BusinessRuleException("El recurso no existe."));
            if (!"Disponible".equals(status)) {
                throw new BusinessRuleException("El recurso no está disponible (estado actual: " + status + ").");
            }
            // table and column names come from the enum, never from user input
            int loanId = Jdbc.insert(conn, "INSERT INTO " + type.table() + " (id_recurso, " + type.borrowerColumn()
                    + ", fecha_prestamo, fecha_devolucion_esperada) VALUES (?,?,?,?)",
                    args(resourceId, borrowerId, loanDate, dueDate));
            Jdbc.update(conn, "UPDATE recursos SET estado_recurso = 'Prestado' WHERE id_recurso = ?", args(resourceId));
            return loanId;
        });
    }

    @Override
    public void giveBack(BorrowerType type, int loanId, LocalDate returnDate) {
        Jdbc.inTransaction("Return resource", conn -> {
            record Loan(int resourceId, String status) { }
            Loan loan = Jdbc.queryOne(conn, "SELECT id_recurso, estado_prestamo FROM " + type.table()
                            + " WHERE id_prestamo = ? FOR UPDATE", args(loanId),
                    rs -> new Loan(rs.getInt(1), rs.getString(2)))
                    .orElseThrow(() -> new BusinessRuleException("El préstamo no existe."));
            if ("Devuelto".equals(loan.status())) {
                throw new BusinessRuleException("Este préstamo ya fue devuelto.");
            }
            Jdbc.update(conn, "UPDATE " + type.table() + " SET estado_prestamo = 'Devuelto', "
                    + "fecha_devolucion_real = ? WHERE id_prestamo = ?", args(returnDate, loanId));
            Jdbc.update(conn, "UPDATE recursos SET estado_recurso = 'Disponible' WHERE id_recurso = ?",
                    args(loan.resourceId()));
            return null;
        });
    }

    @Override
    public int markOverdue(LocalDate today) {
        return Jdbc.inTransaction("Mark overdue loans", conn -> {
            int total = 0;
            for (BorrowerType type : BorrowerType.values()) {
                total += Jdbc.update(conn, "UPDATE " + type.table() + " SET estado_prestamo = 'Vencido' "
                        + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?", args(today));
            }
            return total;
        });
    }

    @Override
    public ReportTable activeLoans(BorrowerType type) {
        String join = type == BorrowerType.STUDENT
                ? "JOIN estudiantes b ON b.id_estudiante = l.id_estudiante"
                : "JOIN profesores b ON b.id_profesor = l.id_profesor";
        return Jdbc.table("Préstamos abiertos (" + type.label().toLowerCase() + "s)",
                List.of("ID préstamo", "Recurso", "Prestatario", "Prestado", "Vence", "Estado"),
                "SELECT l.id_prestamo, CONCAT(r.codigo_recurso, ' - ', r.nombre_recurso), "
                        + "CONCAT_WS(' ', b.primer_nombre, b.primer_apellido, b.segundo_apellido), "
                        + "l.fecha_prestamo, l.fecha_devolucion_esperada, l.estado_prestamo "
                        + "FROM " + type.table() + " l JOIN recursos r ON r.id_recurso = l.id_recurso " + join
                        + " WHERE l.estado_prestamo <> 'Devuelto' ORDER BY l.fecha_devolucion_esperada",
                Jdbc.NO_ARGS);
    }

    @Override
    public ReportTable overdueLoans(LocalDate today) {
        String sql = """
                SELECT 'Estudiante', CONCAT_WS(' ', b.primer_nombre, b.primer_apellido, b.segundo_apellido),
                       r.codigo_recurso, r.nombre_recurso, l.fecha_devolucion_esperada,
                       DATEDIFF(?, l.fecha_devolucion_esperada)
                FROM prestamos_estudiante l JOIN recursos r ON r.id_recurso = l.id_recurso
                     JOIN estudiantes b ON b.id_estudiante = l.id_estudiante
                WHERE l.estado_prestamo <> 'Devuelto' AND l.fecha_devolucion_esperada < ?
                UNION ALL
                SELECT 'Profesor', CONCAT_WS(' ', b.primer_nombre, b.primer_apellido, b.segundo_apellido),
                       r.codigo_recurso, r.nombre_recurso, l.fecha_devolucion_esperada,
                       DATEDIFF(?, l.fecha_devolucion_esperada)
                FROM prestamos_profesor l JOIN recursos r ON r.id_recurso = l.id_recurso
                     JOIN profesores b ON b.id_profesor = l.id_profesor
                WHERE l.estado_prestamo <> 'Devuelto' AND l.fecha_devolucion_esperada < ?
                ORDER BY 6 DESC""";
        return Jdbc.table("Préstamos vencidos", List.of("Tipo", "Prestatario", "Código", "Recurso", "Debía devolver",
                "Días de retraso"), sql, args(today, today, today, today));
    }
}
