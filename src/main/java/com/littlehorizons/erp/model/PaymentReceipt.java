package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Result of registering a payment: invoice id and numbering, totals, resolution used and the invoice lines. */
public record PaymentReceipt(
        int invoiceId,
        String invoiceNumber,
        String receiptNumber,
        LocalDate issueDate,
        String cufe,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal total,
        String resolutionNumber,
        LocalDate resolutionDate,
        String prefix,
        int rangeFrom,
        int rangeTo,
        String gradeName,
        List<InvoiceLine> lines) { }
