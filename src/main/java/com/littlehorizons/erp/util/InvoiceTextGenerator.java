package com.littlehorizons.erp.util;

import com.littlehorizons.erp.model.InvoiceData;
import com.littlehorizons.erp.model.InvoiceLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Builds the plain-text (.txt) representation of an electronic invoice with simulated DIAN elements. */
public final class InvoiceTextGenerator {

    private static final int WIDTH = 92;
    private static final int INNER = WIDTH - 4;
    private static final int QR_MODULES = 25;
    private static final int CUFE_LINE = 48;

    private InvoiceTextGenerator() { }

    /** Writes invoices/factura_000123.txt style files (UTF-8). @return the path of the generated file. */
    public static Path generate(InvoiceData d, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("factura_" + numericPart(d.invoiceNumber()) + ".txt");
        Files.writeString(file, render(d), StandardCharsets.UTF_8);
        return file;
    }

    /** The full text of the invoice, ready to be written to a file or printed on the console. */
    public static String render(InvoiceData d) {
        StringBuilder out = new StringBuilder();
        out.append('╔').append("═".repeat(WIDTH - 2)).append("╗\n");
        out.append(center(SchoolConfig.legalName().toUpperCase()));
        out.append(center("NIT " + SchoolConfig.nit()));
        out.append(center(SchoolConfig.address()));
        out.append(center("Tel. " + SchoolConfig.phone() + "  |  " + SchoolConfig.email()));

        out.append(section("FACTURA ELECTRÓNICA DE VENTA"));
        out.append(row(pair("No. de factura", d.invoiceNumber(), "Fecha de emisión", d.issueDate().toString())));
        out.append(row(pair("Forma de pago", "Contado", "Medio de pago", d.paymentMethod())));
        out.append(row(pair("Recibo de caja", d.receiptNumber(), "Moneda", "COP - Peso colombiano")));
        out.append(row(d.resolutionText()));

        out.append(section("CLIENTE (ACUDIENTE RESPONSABLE DEL PAGO)"));
        out.append(row(field("Nombre", d.customerName())));
        out.append(row(pair("Documento", d.customerDocument(), "Teléfono", d.customerPhone())));
        out.append(row(field("Correo", d.customerEmail())));
        out.append(row(field("Dirección", d.customerAddress())));

        out.append(section("ESTUDIANTE"));
        out.append(row(field("Nombre", d.studentName())));
        out.append(row(pair("Grado", d.gradeName(), "Año académico", String.valueOf(d.academicYear()))));

        out.append(section("DETALLE"));
        out.append(row(String.format("%-3s %-11s %-30s %4s %13s %5s %14s", "#", "Código", "Descripción", "Cant.",
                "Valor unit.", "IVA", "Subtotal")));
        out.append(row("-".repeat(INNER)));
        for (InvoiceLine line : d.lines()) {
            out.append(row(String.format("%-3d %-11s %-30s %4d %13s %4s%% %14s", line.position(),
                    truncate(line.conceptCode(), 11), truncate(line.description(), 30), line.quantity(),
                    money(line.unitValue()), line.vatRate().setScale(0, RoundingMode.HALF_UP), money(line.subtotal()))));
        }

        out.append(section("TOTALES"));
        for (Map.Entry<BigDecimal, BigDecimal[]> entry : taxBreakdown(d.lines()).entrySet()) {
            String label = entry.getKey().signum() == 0 ? "Sin IVA (excluido / exento)"
                    : "Gravado al " + entry.getKey().setScale(0, RoundingMode.HALF_UP) + "%";
            out.append(row(String.format("%-30s base %16s   IVA %16s", label,
                    money(entry.getValue()[0]), money(entry.getValue()[1]))));
        }
        out.append(row("-".repeat(INNER)));
        out.append(row(String.format("%66s %19s", "Subtotal:", money(d.subtotal()))));
        out.append(row(String.format("%66s %19s", "IVA:", money(d.vat()))));
        out.append(row(String.format("%66s %19s", "TOTAL A PAGAR:", money(d.total()))));
        out.append(row("-".repeat(INNER)));
        out.append(row("SON: " + SpanishNumberWords.pesos(d.total())));

        out.append(section("ELEMENTOS DE CUMPLIMIENTO DIAN (SIMULADOS)"));
        out.append(row("CUFE:"));
        for (int i = 0; i < d.cufe().length(); i += CUFE_LINE) {
            out.append(row("  " + d.cufe().substring(i, Math.min(i + CUFE_LINE, d.cufe().length()))));
        }
        out.append(row(""));
        out.append(row("Código QR (contiene número, fecha, NIT, adquirente, valores y CUFE):"));
        for (String qrLine : qr(qrPayload(d))) {
            out.append(centerRaw(qrLine));
        }
        out.append(row(""));

        out.append(section("OBSERVACIONES"));
        out.append(row("Representación gráfica simulada con fines académicos. Sin validez ante la DIAN."));
        out.append(row("Generada por el ERP Little Horizons el "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "."));
        out.append('╚').append("═".repeat(WIDTH - 2)).append("╝\n");
        return out.toString();
    }

    // ---------- tax summary ----------

    /** VAT rate -> {taxable base, VAT amount}, sorted by rate. */
    private static Map<BigDecimal, BigDecimal[]> taxBreakdown(List<InvoiceLine> lines) {
        Map<BigDecimal, BigDecimal[]> result = new TreeMap<>();
        for (InvoiceLine line : lines) {
            BigDecimal[] sums = result.computeIfAbsent(line.vatRate().stripTrailingZeros(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            sums[0] = sums[0].add(line.subtotal());
            sums[1] = sums[1].add(line.vat());
        }
        return result;
    }

    // ---------- QR ----------

    private static String qrPayload(InvoiceData d) {
        return "NumFac=" + d.invoiceNumber() + ";FecFac=" + d.issueDate() + ";NitFac=" + SchoolConfig.nit()
                + ";DocAdq=" + d.customerDocument().replace(" ", "") + ";ValFac=" + d.subtotal().toPlainString()
                + ";ValIva=" + d.vat().toPlainString() + ";ValTotal=" + d.total().toPlainString()
                + ";CUFE=" + d.cufe();
    }

    /** Pseudo QR code made of block characters: three finder squares plus bits derived from a hash. */
    private static List<String> qr(String payload) {
        byte[] bits = expandHash(payload, QR_MODULES * QR_MODULES);
        List<String> lines = new ArrayList<>();
        for (int row = 0; row < QR_MODULES; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < QR_MODULES; col++) {
                sb.append(isDark(row, col, bits) ? "██" : "  ");
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    private static boolean isDark(int row, int col, byte[] bits) {
        int[][] finders = {{0, 0}, {0, QR_MODULES - 7}, {QR_MODULES - 7, 0}};
        for (int[] f : finders) {
            int r = row - f[0];
            int c = col - f[1];
            if (r >= 0 && r < 7 && c >= 0 && c < 7) {
                boolean border = r == 0 || r == 6 || c == 0 || c == 6;
                boolean center = r >= 2 && r <= 4 && c >= 2 && c <= 4;
                return border || center;
            }
            if (r >= -1 && r <= 7 && c >= -1 && c <= 7) {
                return false; // quiet zone around the finder
            }
        }
        return (bits[row * QR_MODULES + col] & 1) == 1;
    }

    private static byte[] expandHash(String source, int length) {
        byte[] out = new byte[length];
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            int filled = 0;
            int counter = 0;
            while (filled < length) {
                sha.update((source + "#" + counter++).getBytes(StandardCharsets.UTF_8));
                byte[] digest = sha.digest();
                for (int i = 0; i < digest.length && filled < length; i++) {
                    out[filled++] = digest[i];
                }
            }
            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    // ---------- box layout helpers ----------

    private static String section(String title) {
        String label = "═ " + title + " ";
        return "╠" + label + "═".repeat(Math.max(0, WIDTH - 2 - label.length())) + "╣\n";
    }

    /** One or more box rows; long text is wrapped at word boundaries. */
    private static String row(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : wrap(text)) {
            out.append("║ ").append(pad(line, INNER)).append(" ║\n");
        }
        return out.toString();
    }

    private static String center(String text) {
        int left = Math.max(0, (INNER - text.length()) / 2);
        return row(" ".repeat(left) + text);
    }

    private static String centerRaw(String text) {
        int left = Math.max(0, (INNER - text.length()) / 2);
        return "║ " + pad(" ".repeat(left) + text, INNER) + " ║\n";
    }

    private static String field(String label, String value) {
        return String.format("%-16s: %s", label, value);
    }

    /** Two label/value pairs on the same row. */
    private static String pair(String label1, String value1, String label2, String value2) {
        return String.format("%-16s: %-26s %-16s: %s", label1, value1, label2, value2);
    }

    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        if (text.length() <= INNER) {
            lines.add(text);
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            while (word.length() > INNER) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, INNER));
                word = word.substring(INNER);
            }
            if (line.length() + word.length() + 1 > INNER && line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    private static String numericPart(String invoiceNumber) {
        return invoiceNumber.substring(invoiceNumber.lastIndexOf('-') + 1);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + ".";
    }

    private static String money(BigDecimal value) {
        return "$ " + NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).format(value);
    }
}
