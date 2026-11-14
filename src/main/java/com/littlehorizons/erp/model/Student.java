package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Domain entity mapped to the "estudiantes" table. */
public record Student(
        Integer id,
        String documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        LocalDate birthDate,
        String gender,
        String address,
        String phone,
        String email,
        LocalDate admissionDate,
        String status) {

    public String getFullName() {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}
