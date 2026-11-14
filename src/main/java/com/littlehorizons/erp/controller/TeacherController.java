package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/** Business rules for teachers, their teaching load and their weekly schedule. */
public class TeacherController {

    private static final LocalTime SCHOOL_OPENS = LocalTime.of(6, 0);
    private static final LocalTime SCHOOL_CLOSES = LocalTime.of(18, 0);

    private final TeacherDao teacherDao;
    private final SubjectDao subjectDao;
    private final TeachingAssignmentDao assignmentDao;

    public TeacherController(TeacherDao teacherDao, SubjectDao subjectDao, TeachingAssignmentDao assignmentDao) {
        this.teacherDao = teacherDao;
        this.subjectDao = subjectDao;
        this.assignmentDao = assignmentDao;
    }

    public int register(Teacher teacher) {
        Validators.documentNumber(teacher.documentNumber());
        Validators.email(teacher.email());
        Validators.phone(teacher.phone());
        if (teacher.hireDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de contratación no puede ser futura.");
        }
        if (teacherDao.findByDocumentNumber(teacher.documentNumber()).isPresent()) {
            throw new BusinessRuleException("Ya existe un profesor con ese documento.");
        }
        if (teacherDao.existsByEmail(teacher.email())) {
            throw new BusinessRuleException("Ya existe un profesor con ese correo electrónico.");
        }
        return teacherDao.insert(teacher);
    }

    public ReportTable list() {
        return teacherDao.listAll();
    }

    public Optional<Teacher> find(String documentNumber) {
        return teacherDao.findByDocumentNumber(documentNumber);
    }

    public Teacher requireByDocument(String documentNumber) {
        return find(documentNumber).orElseThrow(
                () -> new BusinessRuleException("No existe un profesor con el documento " + documentNumber + "."));
    }

    public Subject requireSubject(String code) {
        return subjectDao.findByCode(code.toUpperCase()).orElseThrow(
                () -> new BusinessRuleException("No existe una materia activa con el código " + code + "."));
    }

    public void assignSubject(Teacher teacher, Subject subject, int academicYear, String section) {
        if (!teacher.status().equals("Activo")) {
            throw new BusinessRuleException("Solo se pueden asignar materias a profesores activos.");
        }
        if (assignmentDao.findAssignmentId(teacher.id(), subject.id(), academicYear, section).isPresent()) {
            throw new BusinessRuleException("El profesor ya tiene asignada esta materia en esa sección.");
        }
        assignmentDao.assign(teacher.id(), subject.id(), academicYear, section);
    }

    /** Adds a weekly class slot, refusing overlaps with other classes of the same teacher. */
    public void addScheduleSlot(Teacher teacher, Subject subject, int academicYear, String section, String day,
                                LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (start.isBefore(SCHOOL_OPENS) || end.isAfter(SCHOOL_CLOSES)) {
            throw new BusinessRuleException("Las clases deben estar entre 06:00 y 18:00.");
        }
        int assignmentId = assignmentDao.findAssignmentId(teacher.id(), subject.id(), academicYear, section)
                .orElseThrow(() -> new BusinessRuleException("Primero asigne la materia al profesor."));
        if (assignmentDao.hasScheduleConflict(teacher.id(), academicYear, day, start, end)) {
            throw new BusinessRuleException("Conflicto de horario: el profesor ya tiene una clase en ese rango.");
        }
        assignmentDao.addScheduleSlot(assignmentId, day, start, end);
    }

    public ReportTable assignments(Teacher teacher, int academicYear) {
        return assignmentDao.assignmentsReport(teacher.id(), academicYear);
    }

    public ReportTable schedule(Teacher teacher, int academicYear) {
        return assignmentDao.scheduleReport(teacher.id(), academicYear);
    }
}
