package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportTable;

public interface ReportDao {

    ReportTable courseAverages(int gradeId, int academicYear, String period);

    ReportTable studentsNeedingSupport(int academicYear, String period, java.math.BigDecimal belowAverage);

    ReportTable studentsPerStatus();
}
