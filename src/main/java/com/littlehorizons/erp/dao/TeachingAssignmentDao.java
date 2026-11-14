package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalTime;
import java.util.Optional;

/** Subjects taught by each teacher and their weekly schedule. */
public interface TeachingAssignmentDao {

    int assign(int teacherId, int subjectId, int academicYear, String section);

    Optional<Integer> findAssignmentId(int teacherId, int subjectId, int academicYear, String section);

    boolean isAssigned(int teacherId, int subjectId, int academicYear);

    ReportTable assignmentsReport(int teacherId, int academicYear);

    int addScheduleSlot(int assignmentId, String day, LocalTime start, LocalTime end);

    boolean hasScheduleConflict(int teacherId, int academicYear, String day, LocalTime start, LocalTime end);

    ReportTable scheduleReport(int teacherId, int academicYear);
}
