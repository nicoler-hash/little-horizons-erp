package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.Jdbc;

import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcTeacherDao implements TeacherDao {

    @Override
    public int insert(Teacher t) {
        String sql = """
                INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                  primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion)
                VALUES (?,?,?,?,?,?,?,?,?,?)""";
        return Jdbc.insert(sql, args(t.documentType(), t.documentNumber(), t.firstName(), t.middleName(),
                t.lastName(), t.secondLastName(), t.email(), t.phone(), t.specialization(), t.hireDate()));
    }

    @Override
    public Optional<Teacher> findByDocumentNumber(String documentNumber) {
        return Jdbc.queryOne("SELECT * FROM profesores WHERE numero_documento = ?", args(documentNumber),
                rs -> new Teacher(rs.getInt("id_profesor"), rs.getString("tipo_documento"),
                        rs.getString("numero_documento"), rs.getString("primer_nombre"),
                        rs.getString("segundo_nombre"), rs.getString("primer_apellido"),
                        rs.getString("segundo_apellido"), rs.getString("correo_electronico"),
                        rs.getString("telefono"), rs.getString("especializacion"),
                        Jdbc.date(rs, "fecha_contratacion"), rs.getString("estado")));
    }

    @Override
    public boolean existsByEmail(String email) {
        return Jdbc.exists("SELECT COUNT(*) FROM profesores WHERE correo_electronico = ?", args(email));
    }

    @Override
    public ReportTable listAll() {
        return Jdbc.table("Profesores", List.of("ID", "Documento", "Nombre", "Especialización", "Estado"), """
                SELECT id_profesor, numero_documento,
                       CONCAT_WS(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido),
                       COALESCE(especializacion, '-'), estado
                FROM profesores ORDER BY primer_apellido, primer_nombre""", Jdbc.NO_ARGS);
    }
}
