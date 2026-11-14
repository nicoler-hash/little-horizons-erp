package com.littlehorizons.erp.model;

import com.littlehorizons.erp.util.Names;

import java.time.LocalDate;

/** Teacher ("profesores" table). */
public record Teacher(
        Integer id,
        String documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String email,
        String phone,
        String specialization,
        LocalDate hireDate,
        String status) {

    public String getFullName() {
        return Names.join(firstName, middleName, lastName, secondLastName);
    }
}
