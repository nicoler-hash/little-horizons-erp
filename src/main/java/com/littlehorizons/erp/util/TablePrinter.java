package com.littlehorizons.erp.util;

import com.littlehorizons.erp.model.ReportTable;

import java.util.List;

/** Prints a {@link ReportTable} as an aligned text table. */
public final class TablePrinter {

    private static final int MAX_COLUMN_WIDTH = 38;

    private TablePrinter() { }

    public static void print(ReportTable table) {
        System.out.println("\n== " + table.title() + " ==");
        if (table.isEmpty()) {
            System.out.println("  (Sin resultados)");
            return;
        }
        int columns = table.headers().size();
        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            widths[c] = table.headers().get(c).length();
            for (List<String> row : table.rows()) {
                widths[c] = Math.max(widths[c], row.get(c).length());
            }
            widths[c] = Math.min(widths[c], MAX_COLUMN_WIDTH);
        }
        printRow(table.headers(), widths);
        System.out.println("-".repeat(java.util.Arrays.stream(widths).sum() + 2 * (columns - 1)));
        table.rows().forEach(row -> printRow(row, widths));
        System.out.println("(" + table.rows().size() + " fila" + (table.rows().size() == 1 ? "" : "s") + ")");
    }

    private static void printRow(List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            String cell = cells.get(c);
            if (cell.length() > widths[c]) {
                cell = cell.substring(0, widths[c] - 1) + "…";
            }
            line.append(String.format("%-" + widths[c] + "s", cell));
            if (c < widths.length - 1) {
                line.append("  ");
            }
        }
        System.out.println(line);
    }
}
