package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Subjects catalog and enrollment of students in subjects. */
public interface SubjectDao {

    int insert(Subject subject);

    Optional<Subject> findByCode(String code);

    List<Subject> findActiveByGrade(int gradeId);

    ReportTable listReport(Integer gradeId);

    boolean existsEnrollment(int studentId, int subjectId, int academicYear, String period);

    boolean isEnrolledInYear(int studentId, int subjectId, int academicYear);

    int enrollStudent(int studentId, int subjectId, int academicYear, String period, String section, LocalDate date);

    List<Student> findEnrolledStudents(int subjectId, int academicYear);

    ReportTable enrollmentsOfStudent(int studentId, int academicYear);
}
