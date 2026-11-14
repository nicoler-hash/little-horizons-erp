package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.time.LocalDate;
import java.util.List;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcAttendanceDao implements AttendanceDao {

    @Override
    public void saveAll(List<AttendanceRecord> records) {
        String sql = """
                INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, registrado_por)
                VALUES (?,?,?,?,?)
                ON DUPLICATE KEY UPDATE estado_asistencia = VALUES(estado_asistencia),
                  registrado_por = VALUES(registrado_por)""";
        Jdbc.inTransaction("Save attendance", conn -> {
            for (AttendanceRecord r : records) {
                Jdbc.update(conn, sql, args(r.studentId(), r.subjectId(), r.date(), r.status(), r.teacherId()));
            }
            return null;
        });
    }

    @Override
    public ReportTable absenceAlerts(LocalDate from, LocalDate to, int minAbsencesExclusive) {
        return Jdbc.table("Alertas de inasistencia (más de " + minAbsencesExclusive + " ausencias en una semana)",
                List.of("Documento", "Estudiante", "Semana desde", "Ausencias"), """
                SELECT e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, e.primer_apellido, e.segundo_apellido),
                       MIN(a.fecha_asistencia), COUNT(*)
                FROM asistencias a JOIN estudiantes e ON e.id_estudiante = a.id_estudiante
                WHERE a.estado_asistencia = 'Ausente' AND a.fecha_asistencia BETWEEN ? AND ?
                GROUP BY e.id_estudiante, YEARWEEK(a.fecha_asistencia, 1)
                HAVING COUNT(*) > ?
                ORDER BY COUNT(*) DESC, MIN(a.fecha_asistencia)""", args(from, to, minAbsencesExclusive));
    }

    @Override
    public ReportTable historyOfStudent(int studentId, LocalDate from, LocalDate to) {
        return Jdbc.table("Asistencia del estudiante", List.of("Fecha", "Materia", "Estado"), """
                SELECT a.fecha_asistencia, m.nombre_materia, a.estado_asistencia
                FROM asistencias a JOIN materias m ON m.id_materia = a.id_materia
                WHERE a.id_estudiante = ? AND a.fecha_asistencia BETWEEN ? AND ?
                ORDER BY a.fecha_asistencia DESC, m.nombre_materia""", args(studentId, from, to));
    }
}
