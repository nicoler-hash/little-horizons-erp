package com.littlehorizons.erp.model;

import com.littlehorizons.erp.util.Names;

/** Guardian / legal tutor of one or more students ("acudientes" table). */
public record Guardian(
        Integer id,
        String documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String relationship,
        String mainPhone,
        String email,
        String address,
        boolean financiallyResponsible) {

    public String getFullName() {
        return Names.join(firstName, middleName, lastName, secondLastName);
    }
}
