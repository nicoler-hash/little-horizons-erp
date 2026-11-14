package com.littlehorizons.erp.model;

import java.nio.file.Path;

/** Result of the payment flow: the receipt and the generated invoice file (null if it could not be written). */
public record PaymentOutcome(PaymentReceipt receipt, Path file, String warning) { }
