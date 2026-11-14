package com.littlehorizons.erp.model;

/** Subject of a given grade ("materias" table). */
public record Subject(int id, String code, String name, int gradeId, int weeklyHours) { }
