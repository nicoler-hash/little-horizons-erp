package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcPaymentConceptDao implements PaymentConceptDao {

    @Override
    public List<PaymentConcept> findAllActive() {
        return Jdbc.query("SELECT * FROM conceptos_pago WHERE esta_activo = TRUE ORDER BY id_concepto",
                Jdbc.NO_ARGS, JdbcPaymentConceptDao::mapRow);
    }

    @Override
    public Optional<PaymentConcept> findById(int id) {
        return Jdbc.queryOne("SELECT * FROM conceptos_pago WHERE id_concepto = ? AND esta_activo = TRUE", args(id),
                JdbcPaymentConceptDao::mapRow);
    }

    @Override
    public ReportTable catalogReport() {
        return Jdbc.table("Catálogo de conceptos de pago", List.of("ID", "Código", "Concepto", "Valor base", "IVA %",
                "Recurrente"), """
                SELECT id_concepto, codigo_concepto, nombre_concepto, FORMAT(monto_base, 0), porcentaje_iva,
                       IF(es_recurrente, 'Sí', 'No')
                FROM conceptos_pago WHERE esta_activo = TRUE ORDER BY id_concepto""", Jdbc.NO_ARGS);
    }

    @Override
    public Optional<BigDecimal> findCustomAmount(int studentId, int conceptId) {
        return Jdbc.queryOne("SELECT monto_personalizado FROM valores_concepto_estudiante "
                + "WHERE id_estudiante = ? AND id_concepto = ?", args(studentId, conceptId), rs -> rs.getBigDecimal(1));
    }

    @Override
    public void saveCustomAmount(int studentId, int conceptId, BigDecimal amount, String reason) {
        Jdbc.update("""
                INSERT INTO valores_concepto_estudiante (id_estudiante, id_concepto, monto_personalizado, motivo)
                VALUES (?,?,?,?)
                ON DUPLICATE KEY UPDATE monto_personalizado = VALUES(monto_personalizado), motivo = VALUES(motivo)""",
                args(studentId, conceptId, amount, reason));
    }

    private static PaymentConcept mapRow(ResultSet rs) throws SQLException {
        return new PaymentConcept(rs.getInt("id_concepto"), rs.getString("codigo_concepto"),
                rs.getString("nombre_concepto"), rs.getBigDecimal("monto_base"), rs.getBigDecimal("porcentaje_iva"),
                rs.getBoolean("es_recurrente"));
    }
}
