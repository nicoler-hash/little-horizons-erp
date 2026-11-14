package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Catalog entry of something the school charges for. */
public record PaymentConcept(int id, String code, String name, BigDecimal baseAmount, BigDecimal vatRate,
                             boolean recurring) { }
