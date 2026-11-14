package com.littlehorizons.erp.exception;

/** Thrown when a business rule is violated; the message is safe to show to the user. */
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
