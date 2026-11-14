package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.ClassRoster;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Subjects, subject enrollment, grades and attendance. */
public class AcademicController {

    public static final List<String> PERIODS = List.of("I", "II", "III", "IV");
    public static final List<String> GRADE_PERIODS = List.of("I", "II", "III", "IV", "Final");
    public static final BigDecimal MIN_SCORE = new BigDecimal("0.0");
    public static final BigDecimal MAX_SCORE = new BigDecimal("5.0");
    public static final int ABSENCE_ALERT_THRESHOLD = 3;

    private final SubjectDao subjectDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final EnrollmentDao enrollmentDao;
    private final TeachingAssignmentDao assignmentDao;
    private final AcademicRecordDao recordDao;
    private final AttendanceDao attendanceDao;

    public AcademicController(SubjectDao subjectDao, StudentDao studentDao, TeacherDao teacherDao,
                              EnrollmentDao enrollmentDao, TeachingAssignmentDao assignmentDao,
                              AcademicRecordDao recordDao, AttendanceDao attendanceDao) {
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
        this.enrollmentDao = enrollmentDao;
        this.assignmentDao = assignmentDao;
        this.recordDao = recordDao;
        this.attendanceDao = attendanceDao;
    }

    // ---------- lookups ----------

    public Student requireStudent(String document) {
        return studentDao.findByDocumentNumber(document).orElseThrow(
                () -> new BusinessRuleException("No existe un estudiante con el documento " + document + "."));
    }

    public Teacher requireTeacher(String document) {
        return teacherDao.findByDocumentNumber(document).orElseThrow(
                () -> new BusinessRuleException("No existe un profesor con el documento " + document + "."));
    }

    public Subject requireSubject(String code) {
        return subjectDao.findByCode(code.toUpperCase()).orElseThrow(
                () -> new BusinessRuleException("No existe una materia activa con el código " + code + "."));
    }

    public List<IdName> gradeLevels() {
        return enrollmentDao.findGradeLevels();
    }

    public ReportTable listSubjects(Integer gradeId) {
        return subjectDao.listReport(gradeId);
    }

    // ---------- subject enrollment ----------

    public void enrollInSubject(Student student, Subject subject, int year, String period, String section) {
        int gradeId = activeGradeOf(student, year);
        if (gradeId != subject.gradeId()) {
            throw new BusinessRuleException("La materia no corresponde al grado en que está matriculado el estudiante.");
        }
        if (subjectDao.existsEnrollment(student.id(), subject.id(), year, period)) {
            throw new BusinessRuleException("El estudiante ya está inscrito en esa materia y período.");
        }
        subjectDao.enrollStudent(student.id(), subject.id(), year, period, section, LocalDate.now());
    }

    /** Enrolls the student in every subject of his or her grade for a period. @return how many were added. */
    public int enrollInAllSubjects(Student student, int year, String period, String section) {
        int gradeId = activeGradeOf(student, year);
        List<Subject> pending = subjectDao.findActiveByGrade(gradeId).stream()
                .filter(subject -> !subjectDao.existsEnrollment(student.id(), subject.id(), year, period))
                .toList();
        pending.forEach(subject ->
                subjectDao.enrollStudent(student.id(), subject.id(), year, period, section, LocalDate.now()));
        return pending.size();
    }

    public ReportTable enrollmentsOf(Student student, int year) {
        return subjectDao.enrollmentsOfStudent(student.id(), year);
    }

    private int activeGradeOf(Student student, int year) {
        if (!student.status().equals("Activo")) {
            throw new BusinessRuleException("El estudiante no está activo.");
        }
        return enrollmentDao.findActiveGradeId(student.id(), year).orElseThrow(
                () -> new BusinessRuleException("El estudiante no tiene matrícula activa en " + year + "."));
    }

    // ---------- grades ----------

    /** Saves a grade. Only the teacher assigned to the subject can grade students enrolled in it. */
    public AcademicRecord recordGrade(Teacher teacher, Student student, Subject subject, int year, String period,
                                      BigDecimal score, String notes) {
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0 || score.stripTrailingZeros().scale() > 1) {
            throw new BusinessRuleException("La nota debe estar entre 0.0 y 5.0 con máximo un decimal.");
        }
        if (!GRADE_PERIODS.contains(period)) {
            throw new BusinessRuleException("Período no válido.");
        }
        if (!assignmentDao.isAssigned(teacher.id(), subject.id(), year)) {
            throw new BusinessRuleException("El profesor no dicta esta materia en " + year + ".");
        }
        if (!subjectDao.isEnrolledInYear(student.id(), subject.id(), year)) {
            throw new BusinessRuleException("El estudiante no está inscrito en esta materia en " + year + ".");
        }
        AcademicRecord record = new AcademicRecord(student.id(), subject.id(), teacher.id(), year, period,
                score.setScale(1), notes, LocalDate.now());
        recordDao.save(record);
        return record;
    }

    public ReportTable gradeHistory(Student student) {
        return recordDao.history(student.id());
    }

    public ReportTable subjectAverages(Student student, int year) {
        return recordDao.subjectAverages(student.id(), year);
    }

    public Optional<BigDecimal> generalAverage(Student student, int year, String period) {
        return recordDao.generalAverage(student.id(), year, period);
    }

    // ---------- attendance ----------

    public ClassRoster loadRoster(Teacher teacher, Subject subject, int year) {
        if (!assignmentDao.isAssigned(teacher.id(), subject.id(), year)) {
            throw new BusinessRuleException("El profesor no dicta esta materia en " + year + ".");
        }
        List<Student> students = subjectDao.findEnrolledStudents(subject.id(), year);
        if (students.isEmpty()) {
            throw new BusinessRuleException("No hay estudiantes inscritos en esta materia.");
        }
        return new ClassRoster(teacher, subject, year, students);
    }

    /**
     * Saves the attendance of the whole class: everybody is "Presente" except the students in exceptions
     * (student id -> status).
     */
    public int saveAttendance(ClassRoster roster, LocalDate date, Map<Integer, String> exceptions) {
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("No se puede registrar asistencia de una fecha futura.");
        }
        List<AttendanceRecord> records = new ArrayList<>();
        roster.students().forEach(student -> records.add(new AttendanceRecord(student.id(), roster.subject().id(),
                date, exceptions.getOrDefault(student.id(), "Presente"), roster.teacher().id())));
        attendanceDao.saveAll(records);
        return records.size();
    }

    public ReportTable absenceAlerts(int lastWeeks) {
        LocalDate today = LocalDate.now();
        return attendanceDao.absenceAlerts(today.minusWeeks(lastWeeks), today, ABSENCE_ALERT_THRESHOLD);
    }

    public ReportTable attendanceOf(Student student, int lastDays) {
        LocalDate today = LocalDate.now();
        return attendanceDao.historyOfStudent(student.id(), today.minusDays(lastDays), today);
    }
}
