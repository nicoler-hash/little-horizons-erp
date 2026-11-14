package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcClubDao implements ClubDao {

    @Override
    public int insert(Club c) {
        return Jdbc.insert("INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, "
                + "horario_reunion) VALUES (?,?,?,?,?)",
                args(c.name(), c.description(), c.supervisorId(), c.maxCapacity(), c.meetingSchedule()));
    }

    @Override
    public Optional<Club> findByName(String name) {
        return Jdbc.queryOne("SELECT * FROM clubes WHERE nombre_club = ? AND esta_activo = TRUE", args(name),
                rs -> new Club(rs.getInt("id_club"), rs.getString("nombre_club"), rs.getString("descripcion"),
                        Jdbc.nullableInt(rs, "id_profesor_supervisor"), rs.getInt("capacidad_maxima"),
                        rs.getString("horario_reunion")));
    }

    @Override
    public ReportTable listActive() {
        return Jdbc.table("Clubes activos", List.of("ID", "Club", "Supervisor", "Cupo", "Inscritos"), """
                SELECT c.id_club, c.nombre_club,
                       COALESCE(CONCAT(p.primer_nombre, ' ', p.primer_apellido), '-'),
                       c.capacidad_maxima,
                       (SELECT COUNT(*) FROM membresias_club m
                         WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa')
                FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor
                WHERE c.esta_activo = TRUE ORDER BY c.nombre_club""", Jdbc.NO_ARGS);
    }

    @Override
    public JoinResult join(int studentId, int clubId, LocalDate date) {
        return Jdbc.inTransaction("Join club", conn -> {
            Optional<Integer> capacity = Jdbc.queryOne(conn,
                    "SELECT capacidad_maxima FROM clubes WHERE id_club = ? AND esta_activo = TRUE FOR UPDATE",
                    args(clubId), rs -> rs.getInt(1));
            if (capacity.isEmpty()) {
                return JoinResult.CLUB_NOT_FOUND;
            }
            int already = Jdbc.queryOne(conn, "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? "
                    + "AND id_estudiante = ? AND estado_membresia = 'Activa'", args(clubId, studentId),
                    rs -> rs.getInt(1)).orElse(0);
            if (already > 0) {
                return JoinResult.ALREADY_MEMBER;
            }
            int members = Jdbc.queryOne(conn, "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? "
                    + "AND estado_membresia = 'Activa'", args(clubId), rs -> rs.getInt(1)).orElse(0);
            if (members >= capacity.get()) {
                return JoinResult.CLUB_FULL;
            }
            Jdbc.insert(conn, "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso) VALUES (?,?,?)",
                    args(studentId, clubId, date));
            return JoinResult.JOINED;
        });
    }

    @Override
    public boolean leave(int studentId, int clubId, LocalDate date, String reason) {
        return Jdbc.update("UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, "
                + "motivo_retiro = ? WHERE id_estudiante = ? AND id_club = ? AND estado_membresia = 'Activa'",
                args(date, reason, studentId, clubId)) > 0;
    }

    @Override
    public ReportTable members(int clubId) {
        return Jdbc.table("Miembros activos del club", List.of("Documento", "Estudiante", "Ingreso"), """
                SELECT e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, e.primer_apellido, e.segundo_apellido), m.fecha_ingreso
                FROM membresias_club m JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                WHERE m.id_club = ? AND m.estado_membresia = 'Activa' ORDER BY e.primer_apellido""", args(clubId));
    }

    @Override
    public ReportTable clubsOfStudent(int studentId) {
        return Jdbc.table("Clubes del estudiante", List.of("Club", "Ingreso", "Estado"), """
                SELECT c.nombre_club, m.fecha_ingreso, m.estado_membresia
                FROM membresias_club m JOIN clubes c ON c.id_club = m.id_club
                WHERE m.id_estudiante = ? ORDER BY m.fecha_ingreso""", args(studentId));
    }

    @Override
    public ReportTable participationReport() {
        return Jdbc.table("Participación en clubes", List.of("Club", "Cupo", "Activos", "Retirados", "% ocupación"), """
                SELECT c.nombre_club, c.capacidad_maxima,
                       SUM(m.estado_membresia = 'Activa'), SUM(m.estado_membresia = 'Retirada'),
                       ROUND(100 * SUM(m.estado_membresia = 'Activa') / c.capacidad_maxima, 1)
                FROM clubes c LEFT JOIN membresias_club m ON m.id_club = c.id_club
                WHERE c.esta_activo = TRUE
                GROUP BY c.id_club, c.nombre_club, c.capacidad_maxima
                ORDER BY SUM(m.estado_membresia = 'Activa') DESC, c.nombre_club""", Jdbc.NO_ARGS);
    }
}
