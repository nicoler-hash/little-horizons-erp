package com.littlehorizons.erp.model;

import java.util.List;

/** Read-only tabular result (headers + string rows) used for listings and reports. */
public record ReportTable(String title, List<String> headers, List<List<String>> rows) {

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
