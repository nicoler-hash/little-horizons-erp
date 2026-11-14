package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChargeDao {

    int insert(Charge charge);

    Optional<Charge> findById(int id);

    List<Charge> findPendingByStudent(int studentId);

    ReportTable pendingReport(int studentId);

    /** Students owing charges that expired before the given cut-off date. */
    ReportTable delinquentReport(LocalDate dueBefore, LocalDate today);
}
