package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.exception.BusinessRuleException;

import java.util.regex.Pattern;

/** Shared validation rules used by the controllers. */
final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");

    private Validators() { }

    static void email(String email) {
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new BusinessRuleException("El correo electrónico no tiene un formato válido.");
        }
    }

    static void phone(String phone) {
        if (phone != null && !PHONE.matcher(phone).matches()) {
            throw new BusinessRuleException("El teléfono debe tener entre 7 y 20 dígitos.");
        }
    }

    static void documentNumber(String document) {
        if (document == null || !document.matches("[A-Za-z0-9]{4,20}")) {
            throw new BusinessRuleException("El número de documento debe tener entre 4 y 20 letras o dígitos.");
        }
    }
}
