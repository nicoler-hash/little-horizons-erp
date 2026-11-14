package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.InvoiceData;
import com.littlehorizons.erp.model.InvoiceRequest;
import com.littlehorizons.erp.model.PaymentReceipt;
import com.littlehorizons.erp.model.ReportTable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/** Payments and electronic invoices (one invoice groups one or more payment lines). */
public interface BillingDao {

    /** Computes the simulated CUFE once the invoice number and total are known (inside the transaction). */
    @FunctionalInterface
    interface CufeCalculator {
        String compute(String invoiceNumber, LocalDate issueDate, BigDecimal total);
    }

    /**
     * One atomic transaction: takes the next invoice number, inserts the invoice, one payment per item and marks
     * the settled charges as paid. Throws BusinessRuleException on rule violations.
     */
    PaymentReceipt registerPayment(InvoiceRequest request, CufeCalculator cufeCalculator);

    void updateInvoiceFile(int invoiceId, String fileName, String path);

    void updateInvoiceFileByNumber(String invoiceNumber, String fileName, String path);

    /** Rebuilds everything printed on an invoice from the database. */
    Optional<InvoiceData> findInvoiceData(String invoiceNumber);

    /** The most recent invoices (number, date, student, items, total, file). */
    ReportTable invoiceList();

    ReportTable historyOfStudent(int studentId);

    ReportTable historyOfGuardian(int guardianId);

    ReportTable revenueByConcept(int year);
}
