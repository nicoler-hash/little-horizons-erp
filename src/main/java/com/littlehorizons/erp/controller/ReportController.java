package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;

import java.math.BigDecimal;
import java.util.List;

/** Cross-module reports and statistics. */
public class ReportController {

    public static final BigDecimal SUPPORT_THRESHOLD = new BigDecimal("3.0");

    private final ReportDao reportDao;
    private final EnrollmentDao enrollmentDao;
    private final ClubDao clubDao;
    private final EventDao eventDao;

    public ReportController(ReportDao reportDao, EnrollmentDao enrollmentDao, ClubDao clubDao, EventDao eventDao) {
        this.reportDao = reportDao;
        this.enrollmentDao = enrollmentDao;
        this.clubDao = clubDao;
        this.eventDao = eventDao;
    }

    public List<IdName> gradeLevels() {
        return enrollmentDao.findGradeLevels();
    }

    public ReportTable studentsPerGrade(int year) {
        return enrollmentDao.studentsPerGrade(year);
    }

    public ReportTable courseAverages(int gradeId, int year, String period) {
        return reportDao.courseAverages(gradeId, year, period);
    }

    public ReportTable studentsNeedingSupport(int year, String period) {
        return reportDao.studentsNeedingSupport(year, period, SUPPORT_THRESHOLD);
    }

    public ReportTable clubParticipation() {
        return clubDao.participationReport();
    }

    public ReportTable eventParticipation() {
        return eventDao.participationByType();
    }
}
