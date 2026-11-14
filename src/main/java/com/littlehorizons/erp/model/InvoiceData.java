package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** All the data printed on an invoice. */
public record InvoiceData(
        String invoiceNumber,
        LocalDate issueDate,
        String cufe,
        String resolutionText,
        String customerName,
        String customerDocument,
        String customerAddress,
        String customerPhone,
        String customerEmail,
        String studentName,
        String gradeName,
        int academicYear,
        List<InvoiceLine> lines,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal total,
        String paymentMethod,
        String receiptNumber) {

    /** Text of the (simulated) DIAN invoicing resolution printed on every invoice. */
    public static String resolutionText(String number, LocalDate date, String prefix, int from, int to) {
        return "Resolución DIAN No. " + number + " del " + date + " | Prefijo " + prefix
                + " | Rango autorizado " + from + " a " + to + " (simulada)";
    }

    /** Cash receipt number of an invoice: REC-2026-000073 for FE-000073. */
    public static String receiptNumber(LocalDate date, String invoiceNumber) {
        return "REC-" + date.getYear() + "-" + invoiceNumber.substring(invoiceNumber.lastIndexOf('-') + 1);
    }
}
