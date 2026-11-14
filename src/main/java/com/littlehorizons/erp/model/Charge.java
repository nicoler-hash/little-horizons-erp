package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An amount a student owes ("cargos" table). */
public record Charge(Integer id, int studentId, int conceptId, String description, BigDecimal amount,
                     LocalDate issueDate, LocalDate dueDate, String status) { }
