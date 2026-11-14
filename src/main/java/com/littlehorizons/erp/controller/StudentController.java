package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Student;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Business rules for students, their guardians and their yearly enrollment. */
public class StudentController {

    private static final int MAX_STUDENT_AGE_YEARS = 25;

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;

    public StudentController(StudentDao studentDao, GuardianDao guardianDao, EnrollmentDao enrollmentDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
    }

    // ---------- students ----------

    /** @return the generated id. @throws BusinessRuleException if a rule is violated. */
    public int register(Student student) {
        LocalDate today = LocalDate.now();
        Validators.documentNumber(student.documentNumber());
        Validators.email(student.email());
        Validators.phone(student.phone());
        if (student.birthDate().isAfter(today)) {
            throw new BusinessRuleException("La fecha de nacimiento no puede ser futura.");
        }
        if (student.birthDate().isBefore(today.minusYears(MAX_STUDENT_AGE_YEARS))) {
            throw new BusinessRuleException("La fecha de nacimiento no es plausible para un estudiante.");
        }
        if (student.admissionDate().isBefore(student.birthDate())) {
            throw new BusinessRuleException("La fecha de ingreso no puede ser anterior al nacimiento.");
        }
        if (studentDao.findByDocumentNumber(student.documentNumber()).isPresent()) {
            throw new BusinessRuleException(
                    "Ya existe un estudiante con el documento " + student.documentNumber() + ".");
        }
        return studentDao.insert(student);
    }

    public List<Student> listAll() {
        return studentDao.findAll();
    }

    public Optional<Student> findByDocumentNumber(String documentNumber) {
        return studentDao.findByDocumentNumber(documentNumber);
    }

    public Student requireByDocument(String documentNumber) {
        return studentDao.findByDocumentNumber(documentNumber).orElseThrow(
                () -> new BusinessRuleException("No existe un estudiante con el documento " + documentNumber + "."));
    }

    public void updateContact(int id, String address, String phone, String email) {
        Validators.email(email);
        Validators.phone(phone);
        studentDao.updateContact(id, address, phone, email);
    }

    /** Changes the student status and closes the current enrollment when the student leaves or graduates. */
    public void changeStatus(Student student, String status) {
        studentDao.updateStatus(student.id(), status);
        LocalDate today = LocalDate.now();
        if (status.equals("Retirado")) {
            enrollmentDao.closeEnrollment(student.id(), today.getYear(), "Retirada", today);
        } else if (status.equals("Graduado")) {
            enrollmentDao.closeEnrollment(student.id(), today.getYear(), "Completada", today);
        }
    }

    /** Number of students per status, sorted by status name. */
    public Map<String, Long> countByStatus() {
        return studentDao.findAll().stream()
                .collect(Collectors.groupingBy(Student::status, TreeMap::new, Collectors.counting()));
    }

    // ---------- guardians ----------

    public Optional<Guardian> findGuardian(String documentNumber) {
        return guardianDao.findByDocumentNumber(documentNumber);
    }

    public int registerGuardian(Guardian guardian) {
        Validators.documentNumber(guardian.documentNumber());
        Validators.phone(guardian.mainPhone());
        Validators.email(guardian.email());
        if (guardianDao.findByDocumentNumber(guardian.documentNumber()).isPresent()) {
            throw new BusinessRuleException("Ya existe un acudiente con el documento " + guardian.documentNumber() + ".");
        }
        return guardianDao.insert(guardian);
    }

    public void linkGuardian(int studentId, int guardianId) {
        if (guardianDao.isLinked(studentId, guardianId)) {
            throw new BusinessRuleException("El acudiente ya está asociado a este estudiante.");
        }
        boolean first = guardianDao.findByStudent(studentId).isEmpty();
        guardianDao.linkToStudent(studentId, guardianId, first, true);
    }

    public List<Guardian> guardiansOf(int studentId) {
        return guardianDao.findByStudent(studentId);
    }

    public ReportTable studentsOfGuardian(int guardianId) {
        return guardianDao.studentsOfGuardian(guardianId);
    }

    // ---------- enrollment ----------

    public List<IdName> gradeLevels() {
        return enrollmentDao.findGradeLevels();
    }

    public void enroll(Student student, int gradeId, int academicYear) {
        if (!student.status().equals("Activo")) {
            throw new BusinessRuleException("Solo se pueden matricular estudiantes en estado Activo.");
        }
        if (guardianDao.findByStudent(student.id()).isEmpty()) {
            throw new BusinessRuleException("El estudiante debe tener al menos un acudiente.");
        }
        if (enrollmentDao.existsForYear(student.id(), academicYear)) {
            throw new BusinessRuleException("El estudiante ya está matriculado en " + academicYear + ".");
        }
        enrollmentDao.enroll(student.id(), gradeId, academicYear, LocalDate.now());
    }

    public ReportTable enrollmentHistory(int studentId) {
        return enrollmentDao.history(studentId);
    }
}
