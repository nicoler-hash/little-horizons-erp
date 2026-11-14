package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcEnrollmentDao implements EnrollmentDao {

    @Override
    public List<IdName> findGradeLevels() {
        return Jdbc.query("SELECT id_grado, nombre_grado FROM grados ORDER BY nivel_numerico", Jdbc.NO_ARGS,
                rs -> new IdName(rs.getInt("id_grado"), rs.getString("nombre_grado")));
    }

    @Override
    public boolean existsForYear(int studentId, int academicYear) {
        return Jdbc.exists("SELECT COUNT(*) FROM matriculas WHERE id_estudiante = ? AND anio_academico = ?",
                args(studentId, academicYear));
    }

    @Override
    public int enroll(int studentId, int gradeId, int academicYear, LocalDate date) {
        return Jdbc.insert("INSERT INTO matriculas (id_estudiante, id_grado, anio_academico, fecha_matricula) "
                + "VALUES (?,?,?,?)", args(studentId, gradeId, academicYear, date));
    }

    @Override
    public Optional<Integer> findActiveGradeId(int studentId, int academicYear) {
        return Jdbc.queryOne("SELECT id_grado FROM matriculas WHERE id_estudiante = ? AND anio_academico = ? "
                + "AND estado_matricula = 'Activa'", args(studentId, academicYear), rs -> rs.getInt(1));
    }

    @Override
    public void closeEnrollment(int studentId, int academicYear, String status, LocalDate date) {
        Jdbc.update("UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ? "
                        + "WHERE id_estudiante = ? AND anio_academico = ? AND estado_matricula = 'Activa'",
                args(status, "Retirada".equals(status) ? date : null, studentId, academicYear));
    }

    @Override
    public ReportTable history(int studentId) {
        return Jdbc.table("Historial de matrículas", List.of("Año", "Grado", "Estado", "Fecha matrícula"), """
                SELECT m.anio_academico, g.nombre_grado, m.estado_matricula, m.fecha_matricula
                FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.id_estudiante = ? ORDER BY m.anio_academico""", args(studentId));
    }

    @Override
    public ReportTable studentsPerGrade(int academicYear) {
        return Jdbc.table("Estudiantes por grado (" + academicYear + ")", List.of("Grado", "Estudiantes activos"), """
                SELECT g.nombre_grado, COUNT(m.id_matricula)
                FROM grados g
                LEFT JOIN matriculas m ON m.id_grado = g.id_grado AND m.anio_academico = ?
                     AND m.estado_matricula = 'Activa'
                GROUP BY g.id_grado, g.nombre_grado, g.nivel_numerico ORDER BY g.nivel_numerico""", args(academicYear));
    }
}
