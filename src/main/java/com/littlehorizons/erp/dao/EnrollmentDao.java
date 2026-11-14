package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Grades catalog and yearly enrollment ("matriculas") of students. */
public interface EnrollmentDao {

    List<IdName> findGradeLevels();

    boolean existsForYear(int studentId, int academicYear);

    int enroll(int studentId, int gradeId, int academicYear, LocalDate date);

    Optional<Integer> findActiveGradeId(int studentId, int academicYear);

    void closeEnrollment(int studentId, int academicYear, String status, LocalDate date);

    ReportTable history(int studentId);

    ReportTable studentsPerGrade(int academicYear);
}
