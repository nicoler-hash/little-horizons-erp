package com.littlehorizons.erp.model;

/** Who borrows a resource. Each type maps to its own loans table. */
public enum BorrowerType {
    STUDENT("prestamos_estudiante", "id_estudiante", "Estudiante"),
    TEACHER("prestamos_profesor", "id_profesor", "Profesor");

    private final String table;
    private final String borrowerColumn;
    private final String label;

    BorrowerType(String table, String borrowerColumn, String label) {
        this.table = table;
        this.borrowerColumn = borrowerColumn;
        this.label = label;
    }

    public String table() { return table; }

    public String borrowerColumn() { return borrowerColumn; }

    public String label() { return label; }
}
