package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceDao {

    /** Saves all marks in one transaction (insert or update per student/subject/day). */
    void saveAll(List<AttendanceRecord> records);

    ReportTable absenceAlerts(LocalDate from, LocalDate to, int minAbsencesExclusive);

    ReportTable historyOfStudent(int studentId, LocalDate from, LocalDate to);
}
