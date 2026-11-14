package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Clubs (with capacity control) and school events (with optional competition results). */
public class ExtracurricularController {

    public static final List<String> EVENT_TYPES = List.of("Académico", "Cultural", "Deportivo", "Social", "Científico");
    public static final List<String> POSITIONS = List.of("1er Lugar", "2do Lugar", "3er Lugar", "Participante");

    private final ClubDao clubDao;
    private final EventDao eventDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ExtracurricularController(ClubDao clubDao, EventDao eventDao, StudentDao studentDao,
                                     TeacherDao teacherDao) {
        this.clubDao = clubDao;
        this.eventDao = eventDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    public Student requireStudent(String document) {
        return studentDao.findByDocumentNumber(document).orElseThrow(
                () -> new BusinessRuleException("No existe un estudiante con el documento " + document + "."));
    }

    public Optional<Teacher> findTeacher(String document) {
        return teacherDao.findByDocumentNumber(document);
    }

    // ---------- clubs ----------

    public int createClub(Club club) {
        if (club.maxCapacity() < 1 || club.maxCapacity() > 200) {
            throw new BusinessRuleException("El cupo máximo debe estar entre 1 y 200.");
        }
        if (clubDao.findByName(club.name()).isPresent()) {
            throw new BusinessRuleException("Ya existe un club activo con ese nombre.");
        }
        return clubDao.insert(club);
    }

    public ReportTable listClubs() {
        return clubDao.listActive();
    }

    public void joinClub(Student student, int clubId) {
        if (!student.status().equals("Activo")) {
            throw new BusinessRuleException("Solo los estudiantes activos pueden inscribirse en clubes.");
        }
        switch (clubDao.join(student.id(), clubId, LocalDate.now())) {
            case JOINED -> { }
            case CLUB_NOT_FOUND -> throw new BusinessRuleException("El club no existe o no está activo.");
            case ALREADY_MEMBER -> throw new BusinessRuleException("El estudiante ya es miembro activo de este club.");
            case CLUB_FULL -> throw new BusinessRuleException("El club alcanzó su cupo máximo.");
        }
    }

    public void leaveClub(Student student, int clubId, String reason) {
        if (!clubDao.leave(student.id(), clubId, LocalDate.now(), reason)) {
            throw new BusinessRuleException("El estudiante no es miembro activo de ese club.");
        }
    }

    public ReportTable clubMembers(int clubId) {
        return clubDao.members(clubId);
    }

    public ReportTable clubsOf(Student student) {
        return clubDao.clubsOfStudent(student.id());
    }

    // ---------- events ----------

    public int createEvent(SchoolEvent event) {
        if (!EVENT_TYPES.contains(event.type())) {
            throw new BusinessRuleException("Tipo de evento no válido.");
        }
        return eventDao.insert(event);
    }

    public ReportTable listEvents() {
        return eventDao.listAll();
    }

    public SchoolEvent requireEvent(int eventId) {
        return eventDao.findById(eventId).orElseThrow(() -> new BusinessRuleException("El evento no existe."));
    }

    public void addParticipant(SchoolEvent event, Student student, String role, String position) {
        if (eventDao.existsParticipant(event.id(), student.id())) {
            throw new BusinessRuleException("El estudiante ya está registrado en este evento.");
        }
        if (position != null && !event.hasCompetition()) {
            throw new BusinessRuleException("Este evento no tiene competencia: no se registran posiciones.");
        }
        if (position != null && !POSITIONS.contains(position)) {
            throw new BusinessRuleException("Posición no válida.");
        }
        eventDao.addParticipant(event.id(), student.id(), role, position);
    }

    public ReportTable eventParticipants(int eventId) {
        return eventDao.participants(eventId);
    }

    public ReportTable eventsOf(Student student) {
        return eventDao.participationOfStudent(student.id());
    }
}
