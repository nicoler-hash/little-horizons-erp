package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.OperationCancelledException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Validated console input: keeps asking until the user provides valid data.
 * Typing "/q" in any prompt cancels the current operation and goes back to the menu.
 */
public final class ConsoleInput {

    public static final String CANCEL_COMMAND = "/q";

    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleInput() { }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SCANNER.hasNextLine()) {
            throw new OperationCancelledException();
        }
        String line = SCANNER.nextLine().trim();
        if (line.equalsIgnoreCase(CANCEL_COMMAND)) {
            throw new OperationCancelledException();
        }
        return line;
    }

    public static String readText(String prompt, boolean required) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty() || !required) {
                return value.isEmpty() ? null : value;
            }
            System.out.println("  ✗ Este campo es obligatorio.");
        }
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            try {
                int number = Integer.parseInt(readLine(prompt));
                if (number >= min && number <= max) {
                    return number;
                }
                System.out.printf("  ✗ Ingrese un número entre %d y %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Ingrese un número entero válido.");
            }
        }
    }

    /** Reads a decimal number (dot or comma accepted) within [min, max] and with at most maxScale decimals. */
    public static BigDecimal readDecimal(String prompt, BigDecimal min, BigDecimal max, int maxScale) {
        while (true) {
            try {
                BigDecimal value = new BigDecimal(readLine(prompt).replace(',', '.'));
                if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                    System.out.printf("  ✗ El valor debe estar entre %s y %s.%n", min.toPlainString(), max.toPlainString());
                } else if (value.stripTrailingZeros().scale() > maxScale) {
                    System.out.printf("  ✗ Máximo %d decimal(es).%n", maxScale);
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Ingrese un número válido (ej: 4.5).");
            }
        }
    }

    /** Reads a date in yyyy-MM-dd format. */
    public static LocalDate readDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(readLine(prompt + " (AAAA-MM-DD): "));
            } catch (DateTimeParseException e) {
                System.out.println("  ✗ Fecha inválida. Ejemplo: 2026-03-27");
            }
        }
    }

    /** Same as {@link #readDate} but ENTER means today. */
    public static LocalDate readDateOrToday(String prompt) {
        while (true) {
            String value = readLine(prompt + " (AAAA-MM-DD, ENTER = hoy): ");
            if (value.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  ✗ Fecha inválida. Ejemplo: 2026-03-27");
            }
        }
    }

    /** Reads a time in HH:mm format. */
    public static LocalTime readTime(String prompt) {
        while (true) {
            try {
                return LocalTime.parse(readLine(prompt + " (HH:mm): "));
            } catch (DateTimeParseException e) {
                System.out.println("  ✗ Hora inválida. Ejemplo: 07:30");
            }
        }
    }

    /** Forces the user to type one of the allowed values (case-insensitive). */
    public static String readOption(String prompt, String... allowed) {
        while (true) {
            String value = readLine(String.format("%s [%s]: ", prompt, String.join("/", allowed)));
            for (String option : allowed) {
                if (option.equalsIgnoreCase(value)) {
                    return option;
                }
            }
            System.out.println("  ✗ Opción no válida: " + Arrays.toString(allowed));
        }
    }

    /** Shows a numbered list and returns the chosen element. */
    public static String readChoice(String prompt, String... options) {
        System.out.println(prompt);
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, options[i]);
        }
        return options[readInt("Elija una opción: ", 1, options.length) - 1];
    }

    public static boolean confirm(String prompt) {
        return readOption(prompt, "S", "N").equals("S");
    }
}
