package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.Jdbc;

import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcEventDao implements EventDao {

    @Override
    public int insert(SchoolEvent e) {
        return Jdbc.insert("INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
                + "tiene_competencia, id_organizador) VALUES (?,?,?,?,?,?,?)",
                args(e.name(), e.description(), e.type(), e.date(), e.place(), e.hasCompetition(), e.organizerId()));
    }

    @Override
    public Optional<SchoolEvent> findById(int id) {
        return Jdbc.queryOne("SELECT * FROM eventos WHERE id_evento = ? AND esta_activo = TRUE", args(id),
                rs -> new SchoolEvent(rs.getInt("id_evento"), rs.getString("nombre_evento"),
                        rs.getString("descripcion"), rs.getString("tipo_evento"), Jdbc.date(rs, "fecha_evento"),
                        rs.getString("lugar"), rs.getBoolean("tiene_competencia"),
                        Jdbc.nullableInt(rs, "id_organizador")));
    }

    @Override
    public ReportTable listAll() {
        return Jdbc.table("Calendario de eventos", List.of("ID", "Fecha", "Evento", "Tipo", "Lugar", "Competencia"), """
                SELECT id_evento, fecha_evento, nombre_evento, tipo_evento, COALESCE(lugar, '-'),
                       IF(tiene_competencia, 'Sí', 'No')
                FROM eventos WHERE esta_activo = TRUE ORDER BY fecha_evento""", Jdbc.NO_ARGS);
    }

    @Override
    public boolean existsParticipant(int eventId, int studentId) {
        return Jdbc.exists("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND id_estudiante = ?",
                args(eventId, studentId));
    }

    @Override
    public void addParticipant(int eventId, int studentId, String role, String position) {
        Jdbc.insert("INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, "
                + "posicion_obtenida) VALUES (?,?,?,?)", args(eventId, studentId, role, position));
    }

    @Override
    public ReportTable participants(int eventId) {
        return Jdbc.table("Participantes del evento", List.of("Documento", "Estudiante", "Rol", "Posición"), """
                SELECT e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, e.primer_apellido, e.segundo_apellido),
                       p.rol_participacion, COALESCE(p.posicion_obtenida, '-')
                FROM participantes_evento p JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                WHERE p.id_evento = ? ORDER BY p.posicion_obtenida, e.primer_apellido""", args(eventId));
    }

    @Override
    public ReportTable participationOfStudent(int studentId) {
        return Jdbc.table("Eventos del estudiante", List.of("Fecha", "Evento", "Tipo", "Posición"), """
                SELECT ev.fecha_evento, ev.nombre_evento, ev.tipo_evento, COALESCE(p.posicion_obtenida, '-')
                FROM participantes_evento p JOIN eventos ev ON ev.id_evento = p.id_evento
                WHERE p.id_estudiante = ? ORDER BY ev.fecha_evento""", args(studentId));
    }

    @Override
    public ReportTable participationByType() {
        return Jdbc.table("Participación por tipo de evento", List.of("Tipo", "Eventos", "Participaciones"), """
                SELECT ev.tipo_evento, COUNT(DISTINCT ev.id_evento), COUNT(p.id_participacion)
                FROM eventos ev LEFT JOIN participantes_evento p ON p.id_evento = ev.id_evento
                WHERE ev.esta_activo = TRUE GROUP BY ev.tipo_evento ORDER BY COUNT(p.id_participacion) DESC""",
                Jdbc.NO_ARGS);
    }
}
