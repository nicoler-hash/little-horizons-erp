package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Writes an amount of Colombian pesos in words, as printed at the bottom of invoices. */
public final class SpanishNumberWords {

    private static final String[] UNITS = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS",
            "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] TENS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
            "OCHENTA", "NOVENTA"};
    private static final String[] HUNDREDS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private SpanishNumberWords() { }

    /** Example: 297500.00 -> "DOSCIENTOS NOVENTA Y SIETE MIL QUINIENTOS PESOS M/CTE". */
    public static String pesos(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long pesos = rounded.toBigInteger().longValue();
        int cents = rounded.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String text;
        if (pesos == 0) {
            text = "CERO PESOS";
        } else if (pesos == 1) {
            text = "UN PESO";
        } else if (pesos % 1_000_000 == 0) {
            text = words(pesos, false) + " DE PESOS";
        } else {
            text = words(pesos, false) + " PESOS";
        }
        if (cents > 0) {
            text += String.format(" CON %02d/100", cents);
        }
        return text + " M/CTE";
    }

    /** @param apocope true when the number precedes MIL / MILLONES ("UN" instead of "UNO") */
    private static String words(long n, boolean apocope) {
        String result;
        if (n >= 1_000_000) {
            long millions = n / 1_000_000;
            long rest = n % 1_000_000;
            String head = millions == 1 ? "UN MILLÓN" : words(millions, true) + " MILLONES";
            result = rest > 0 ? head + " " + words(rest, false) : head;
        } else if (n >= 1000) {
            long thousands = n / 1000;
            long rest = n % 1000;
            String head = thousands == 1 ? "MIL" : words(thousands, true) + " MIL";
            result = rest > 0 ? head + " " + words(rest, false) : head;
        } else {
            result = below1000((int) n);
        }
        return apocope ? applyApocope(result) : result;
    }

    private static String below1000(int n) {
        if (n == 100) {
            return "CIEN";
        }
        StringBuilder sb = new StringBuilder();
        int hundreds = n / 100;
        int rest = n % 100;
        if (hundreds > 0) {
            sb.append(HUNDREDS[hundreds]);
        }
        if (rest > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (rest < 30) {
                sb.append(UNITS[rest]);
            } else {
                sb.append(TENS[rest / 10]);
                if (rest % 10 > 0) {
                    sb.append(" Y ").append(UNITS[rest % 10]);
                }
            }
        }
        return sb.toString();
    }

    private static String applyApocope(String text) {
        if (text.endsWith("VEINTIUNO")) {
            return text.substring(0, text.length() - 9) + "VEINTIÚN";
        }
        return text.endsWith("UNO") ? text.substring(0, text.length() - 1) : text;
    }
}
