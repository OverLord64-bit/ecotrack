package com.ecotrack.report.common;

/** Input that is syntactically fine but not acceptable (for example a rejection without a comment): HTTP 422. */
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}
