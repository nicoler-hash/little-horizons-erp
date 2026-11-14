package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.ReportTable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentConceptDao {

    List<PaymentConcept> findAllActive();

    Optional<PaymentConcept> findById(int id);

    ReportTable catalogReport();

    Optional<BigDecimal> findCustomAmount(int studentId, int conceptId);

    void saveCustomAmount(int studentId, int conceptId, BigDecimal amount, String reason);
}
