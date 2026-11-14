package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcGuardianDao implements GuardianDao {

    @Override
    public int insert(Guardian g) {
        String sql = """
                INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                  primer_apellido, segundo_apellido, parentesco, telefono_principal, correo_electronico,
                  direccion, es_responsable_financiero)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)""";
        return Jdbc.insert(sql, args(g.documentType(), g.documentNumber(), g.firstName(), g.middleName(),
                g.lastName(), g.secondLastName(), g.relationship(), g.mainPhone(), g.email(), g.address(),
                g.financiallyResponsible()));
    }

    @Override
    public Optional<Guardian> findByDocumentNumber(String documentNumber) {
        return Jdbc.queryOne("SELECT * FROM acudientes WHERE numero_documento = ?", args(documentNumber),
                JdbcGuardianDao::mapRow);
    }

    @Override
    public void linkToStudent(int studentId, int guardianId, boolean main, boolean emergencyContact) {
        Jdbc.update("INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, "
                + "es_contacto_emergencia) VALUES (?,?,?,?)", args(studentId, guardianId, main, emergencyContact));
    }

    @Override
    public boolean isLinked(int studentId, int guardianId) {
        return Jdbc.exists("SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?",
                args(studentId, guardianId));
    }

    @Override
    public List<Guardian> findByStudent(int studentId) {
        return Jdbc.query("""
                SELECT a.* FROM acudientes a
                JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente
                WHERE ea.id_estudiante = ?
                ORDER BY ea.es_principal DESC, a.primer_apellido""", args(studentId), JdbcGuardianDao::mapRow);
    }

    @Override
    public ReportTable studentsOfGuardian(int guardianId) {
        return Jdbc.table("Estudiantes a cargo", List.of("ID", "Documento", "Estudiante", "Estado"), """
                SELECT e.id_estudiante, e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido),
                       e.estado
                FROM estudiantes e JOIN estudiante_acudiente ea ON ea.id_estudiante = e.id_estudiante
                WHERE ea.id_acudiente = ? ORDER BY e.primer_apellido""", args(guardianId));
    }

    private static Guardian mapRow(ResultSet rs) throws SQLException {
        return new Guardian(rs.getInt("id_acudiente"), rs.getString("tipo_documento"),
                rs.getString("numero_documento"), rs.getString("primer_nombre"), rs.getString("segundo_nombre"),
                rs.getString("primer_apellido"), rs.getString("segundo_apellido"), rs.getString("parentesco"),
                rs.getString("telefono_principal"), rs.getString("correo_electronico"), rs.getString("direccion"),
                rs.getBoolean("es_responsable_financiero"));
    }
}
