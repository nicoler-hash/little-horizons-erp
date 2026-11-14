package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcChargeDao implements ChargeDao {

    @Override
    public int insert(Charge c) {
        return Jdbc.insert("INSERT INTO cargos (id_estudiante, id_concepto, descripcion, valor, fecha_emision, "
                + "fecha_vencimiento) VALUES (?,?,?,?,?,?)",
                args(c.studentId(), c.conceptId(), c.description(), c.amount(), c.issueDate(), c.dueDate()));
    }

    @Override
    public Optional<Charge> findById(int id) {
        return Jdbc.queryOne("SELECT * FROM cargos WHERE id_cargo = ?", args(id), JdbcChargeDao::mapRow);
    }

    @Override
    public List<Charge> findPendingByStudent(int studentId) {
        return Jdbc.query("SELECT * FROM cargos WHERE id_estudiante = ? AND estado = 'Pendiente' "
                + "ORDER BY fecha_vencimiento", args(studentId), JdbcChargeDao::mapRow);
    }

    @Override
    public ReportTable pendingReport(int studentId) {
        return Jdbc.table("Cargos pendientes", List.of("ID", "Descripción", "Valor", "Vence", "Días de mora"), """
                SELECT id_cargo, descripcion, FORMAT(valor, 0), fecha_vencimiento,
                       GREATEST(DATEDIFF(CURDATE(), fecha_vencimiento), 0)
                FROM cargos WHERE id_estudiante = ? AND estado = 'Pendiente' ORDER BY fecha_vencimiento""",
                args(studentId));
    }

    @Override
    public ReportTable delinquentReport(LocalDate dueBefore, LocalDate today) {
        return Jdbc.table("Estudiantes morosos", List.of("Documento", "Estudiante", "Cargos vencidos",
                "Total adeudado", "Vencimiento más antiguo", "Días de mora"), """
                SELECT e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido),
                       COUNT(*), FORMAT(SUM(c.valor), 0), MIN(c.fecha_vencimiento),
                       DATEDIFF(?, MIN(c.fecha_vencimiento))
                FROM cargos c JOIN estudiantes e ON e.id_estudiante = c.id_estudiante
                WHERE c.estado = 'Pendiente' AND c.fecha_vencimiento < ?
                GROUP BY e.id_estudiante
                ORDER BY MIN(c.fecha_vencimiento), e.primer_apellido""", args(today, dueBefore));
    }

    private static Charge mapRow(ResultSet rs) throws SQLException {
        return new Charge(rs.getInt("id_cargo"), rs.getInt("id_estudiante"), rs.getInt("id_concepto"),
                rs.getString("descripcion"), rs.getBigDecimal("valor"), Jdbc.date(rs, "fecha_emision"),
                Jdbc.date(rs, "fecha_vencimiento"), rs.getString("estado"));
    }
}
