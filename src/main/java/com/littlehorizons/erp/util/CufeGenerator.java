package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

/** Simulated CUFE: SHA-384 of invoice number + NIT + issue date + total (96 hex characters). */
public final class CufeGenerator {

    private CufeGenerator() { }

    public static String generate(String invoiceNumber, String nit, LocalDate issueDate, BigDecimal total) {
        String source = String.join("|", invoiceNumber, nit, issueDate.toString(),
                total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-384").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 is not available", e);
        }
    }
}
