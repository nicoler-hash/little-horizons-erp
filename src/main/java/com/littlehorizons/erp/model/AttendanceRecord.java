package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** One attendance mark of a student in a subject on a given day. */
public record AttendanceRecord(int studentId, int subjectId, LocalDate date, String status, int teacherId) { }
