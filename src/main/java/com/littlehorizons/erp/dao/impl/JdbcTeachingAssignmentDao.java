package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcTeachingAssignmentDao implements TeachingAssignmentDao {

    @Override
    public int assign(int teacherId, int subjectId, int academicYear, String section) {
        return Jdbc.insert("INSERT INTO asignaciones_profesor (id_profesor, id_materia, anio_academico, seccion) "
                + "VALUES (?,?,?,?)", args(teacherId, subjectId, academicYear, section));
    }

    @Override
    public Optional<Integer> findAssignmentId(int teacherId, int subjectId, int academicYear, String section) {
        return Jdbc.queryOne("SELECT id_asignacion FROM asignaciones_profesor WHERE id_profesor = ? "
                        + "AND id_materia = ? AND anio_academico = ? AND seccion = ?",
                args(teacherId, subjectId, academicYear, section), rs -> rs.getInt(1));
    }

    @Override
    public boolean isAssigned(int teacherId, int subjectId, int academicYear) {
        return Jdbc.exists("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? AND id_materia = ? "
                + "AND anio_academico = ?", args(teacherId, subjectId, academicYear));
    }

    @Override
    public ReportTable assignmentsReport(int teacherId, int academicYear) {
        return Jdbc.table("Materias asignadas", List.of("Código", "Materia", "Grado", "Sección"), """
                SELECT m.codigo_materia, m.nombre_materia, g.nombre_grado, COALESCE(ap.seccion, '-')
                FROM asignaciones_profesor ap
                JOIN materias m ON m.id_materia = ap.id_materia
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE ap.id_profesor = ? AND ap.anio_academico = ?
                ORDER BY g.nivel_numerico, m.nombre_materia""", args(teacherId, academicYear));
    }

    @Override
    public int addScheduleSlot(int assignmentId, String day, LocalTime start, LocalTime end) {
        return Jdbc.insert("INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?,?,?,?)", args(assignmentId, day, start, end));
    }

    @Override
    public boolean hasScheduleConflict(int teacherId, int academicYear, String day, LocalTime start, LocalTime end) {
        return Jdbc.exists("""
                SELECT COUNT(*) FROM horarios_clase h
                JOIN asignaciones_profesor ap ON ap.id_asignacion = h.id_asignacion
                WHERE ap.id_profesor = ? AND ap.anio_academico = ? AND h.dia_semana = ?
                  AND h.hora_inicio < ? AND h.hora_fin > ?""", args(teacherId, academicYear, day, end, start));
    }

    @Override
    public ReportTable scheduleReport(int teacherId, int academicYear) {
        return Jdbc.table("Horario semanal", List.of("Día", "Inicio", "Fin", "Materia", "Grado"), """
                SELECT h.dia_semana, TIME_FORMAT(h.hora_inicio, '%H:%i'), TIME_FORMAT(h.hora_fin, '%H:%i'),
                       m.nombre_materia, g.nombre_grado
                FROM horarios_clase h
                JOIN asignaciones_profesor ap ON ap.id_asignacion = h.id_asignacion
                JOIN materias m ON m.id_materia = ap.id_materia
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE ap.id_profesor = ? AND ap.anio_academico = ?
                ORDER BY FIELD(h.dia_semana, 'Lunes','Martes','Miercoles','Jueves','Viernes'), h.hora_inicio""",
                args(teacherId, academicYear));
    }
}
