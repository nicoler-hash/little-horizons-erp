package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade (score 0.0 - 5.0) of a student in a subject and academic period. */
public record AcademicRecord(
        int studentId,
        int subjectId,
        int teacherId,
        int academicYear,
        String period,
        BigDecimal score,
        String notes,
        LocalDate recordDate) { }
