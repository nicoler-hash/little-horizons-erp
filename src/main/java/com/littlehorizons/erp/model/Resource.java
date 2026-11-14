package com.littlehorizons.erp.model;

/** Lendable resource ("recursos" table). */
public record Resource(Integer id, String code, String name, int categoryId, String description,
                       String location, String status) { }
