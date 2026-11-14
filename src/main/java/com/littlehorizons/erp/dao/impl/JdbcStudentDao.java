package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

/** JDBC implementation of {@link StudentDao}. */
public class JdbcStudentDao implements StudentDao {

    private static final String COLUMNS = """
            id_estudiante, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
            primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono,
            correo_electronico, fecha_ingreso, estado""";

    @Override
    public int insert(Student s) {
        String sql = """
                INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                  primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono,
                  correo_electronico, fecha_ingreso)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""";
        return Jdbc.insert(sql, args(s.documentType(), s.documentNumber(), s.firstName(), s.middleName(),
                s.lastName(), s.secondLastName(), s.birthDate(), s.gender(), s.address(), s.phone(),
                s.email(), s.admissionDate()));
    }

    @Override
    public Optional<Student> findByDocumentNumber(String documentNumber) {
        return Jdbc.queryOne("SELECT " + COLUMNS + " FROM estudiantes WHERE numero_documento = ?",
                args(documentNumber), JdbcStudentDao::mapRow);
    }

    @Override
    public List<Student> findAll() {
        return Jdbc.query("SELECT " + COLUMNS
                        + " FROM estudiantes ORDER BY primer_apellido, segundo_apellido, primer_nombre",
                Jdbc.NO_ARGS, JdbcStudentDao::mapRow);
    }

    @Override
    public boolean updateContact(int id, String address, String phone, String email) {
        return Jdbc.update("UPDATE estudiantes SET direccion = ?, telefono = ?, correo_electronico = ? "
                + "WHERE id_estudiante = ?", args(address, phone, email, id)) > 0;
    }

    /** Soft delete: students are never removed, their payments and grades must be kept. */
    @Override
    public boolean updateStatus(int id, String status) {
        return Jdbc.update("UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", args(status, id)) > 0;
    }

    /** Maps a row that contains the columns of the estudiantes table. */
    public static Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("id_estudiante"),
                rs.getString("tipo_documento"),
                rs.getString("numero_documento"),
                rs.getString("primer_nombre"),
                rs.getString("segundo_nombre"),
                rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"),
                Jdbc.date(rs, "fecha_nacimiento"),
                rs.getString("genero"),
                rs.getString("direccion"),
                rs.getString("telefono"),
                rs.getString("correo_electronico"),
                Jdbc.date(rs, "fecha_ingreso"),
                rs.getString("estado"));
    }
}
