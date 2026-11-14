package com.littlehorizons.erp.exception;

/** Unchecked wrapper for SQL failures, so upper layers do not depend on JDBC. */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
