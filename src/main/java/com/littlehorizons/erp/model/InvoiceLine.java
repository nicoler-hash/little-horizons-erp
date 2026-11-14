package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** One line of an invoice. The amount paid for the line includes VAT; subtotal and VAT are derived from it. */
public record InvoiceLine(
        int position,
        String conceptCode,
        String description,
        int quantity,
        BigDecimal unitValue,
        BigDecimal vatRate,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal total) {

    /** Builds a line from the total paid (VAT included) and the VAT rate (percentage) of the concept. */
    public static InvoiceLine of(int position, String conceptCode, String description, int quantity,
                                 BigDecimal totalPaid, BigDecimal vatRatePercent) {
        BigDecimal total = totalPaid.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(BigDecimal.ONE.add(vatRatePercent.movePointLeft(2)), 2, RoundingMode.HALF_UP);
        BigDecimal unit = subtotal.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        return new InvoiceLine(position, conceptCode, description, quantity, unit, vatRatePercent, subtotal,
                total.subtract(subtotal), total);
    }
}
