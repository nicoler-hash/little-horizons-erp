package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcSubjectDao implements SubjectDao {

    @Override
    public int insert(Subject s) {
        return Jdbc.insert("INSERT INTO materias (codigo_materia, nombre_materia, id_grado, horas_semanales) "
                + "VALUES (?,?,?,?)", args(s.code(), s.name(), s.gradeId(), s.weeklyHours()));
    }

    @Override
    public Optional<Subject> findByCode(String code) {
        return Jdbc.queryOne("SELECT * FROM materias WHERE codigo_materia = ? AND esta_activa = TRUE", args(code),
                JdbcSubjectDao::mapRow);
    }

    @Override
    public List<Subject> findActiveByGrade(int gradeId) {
        return Jdbc.query("SELECT * FROM materias WHERE id_grado = ? AND esta_activa = TRUE ORDER BY nombre_materia",
                args(gradeId), JdbcSubjectDao::mapRow);
    }

    @Override
    public ReportTable listReport(Integer gradeId) {
        return Jdbc.table("Materias", List.of("Código", "Materia", "Grado", "Horas/sem"), """
                SELECT m.codigo_materia, m.nombre_materia, g.nombre_grado, m.horas_semanales
                FROM materias m JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.esta_activa = TRUE AND (? IS NULL OR m.id_grado = ?)
                ORDER BY g.nivel_numerico, m.nombre_materia""", args(gradeId, gradeId));
    }

    @Override
    public boolean existsEnrollment(int studentId, int subjectId, int academicYear, String period) {
        return Jdbc.exists("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? "
                + "AND anio_academico = ? AND periodo_academico = ?", args(studentId, subjectId, academicYear, period));
    }

    @Override
    public boolean isEnrolledInYear(int studentId, int subjectId, int academicYear) {
        return Jdbc.exists("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? "
                + "AND anio_academico = ? AND estado <> 'Retirada'", args(studentId, subjectId, academicYear));
    }

    @Override
    public int enrollStudent(int studentId, int subjectId, int academicYear, String period, String section,
                             LocalDate date) {
        return Jdbc.insert("INSERT INTO inscripciones_materia (id_estudiante, id_materia, anio_academico, "
                        + "periodo_academico, seccion, fecha_inscripcion) VALUES (?,?,?,?,?,?)",
                args(studentId, subjectId, academicYear, period, section, date));
    }

    @Override
    public List<Student> findEnrolledStudents(int subjectId, int academicYear) {
        return Jdbc.query("""
                SELECT DISTINCT e.* FROM estudiantes e
                JOIN inscripciones_materia im ON im.id_estudiante = e.id_estudiante
                WHERE im.id_materia = ? AND im.anio_academico = ? AND im.estado <> 'Retirada'
                ORDER BY e.primer_apellido, e.primer_nombre""", args(subjectId, academicYear),
                JdbcStudentDao::mapRow);
    }

    @Override
    public ReportTable enrollmentsOfStudent(int studentId, int academicYear) {
        return Jdbc.table("Materias inscritas", List.of("Código", "Materia", "Período", "Sección", "Estado"), """
                SELECT m.codigo_materia, m.nombre_materia, im.periodo_academico, COALESCE(im.seccion, '-'), im.estado
                FROM inscripciones_materia im JOIN materias m ON m.id_materia = im.id_materia
                WHERE im.id_estudiante = ? AND im.anio_academico = ?
                ORDER BY FIELD(im.periodo_academico, 'I','II','III','IV'), m.nombre_materia""",
                args(studentId, academicYear));
    }

    private static Subject mapRow(ResultSet rs) throws SQLException {
        return new Subject(rs.getInt("id_materia"), rs.getString("codigo_materia"), rs.getString("nombre_materia"),
                rs.getInt("id_grado"), rs.getInt("horas_semanales"));
    }
}
