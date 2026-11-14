package com.littlehorizons.erp.exception;

/** Thrown when the user types the cancel command (/q) in any prompt. */
public class OperationCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationCancelledException() {
        super("Operation cancelled by the user");
    }
}
