package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** One thing being paid: a concept (optionally settling a charge), a quantity and the unit price (VAT included). */
public record PaymentItem(int conceptId, Integer chargeId, int quantity, BigDecimal unitAmount, String description,
                          String period) { }
