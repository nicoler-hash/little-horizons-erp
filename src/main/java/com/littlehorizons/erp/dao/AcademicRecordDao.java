package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.ReportTable;

import java.math.BigDecimal;
import java.util.Optional;

public interface AcademicRecordDao {

    /** Inserts the grade, or updates it if one already exists for that student/subject/year/period. */
    void save(AcademicRecord record);

    ReportTable history(int studentId);

    ReportTable subjectAverages(int studentId, int academicYear);

    Optional<BigDecimal> generalAverage(int studentId, int academicYear, String period);
}
