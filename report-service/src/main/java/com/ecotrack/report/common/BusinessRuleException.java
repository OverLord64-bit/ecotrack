package com.ecotrack.report.common;

/** A well-formed request that conflicts with a business rule or the current state: HTTP 409. */
public class BusinessRuleException extends RuntimeException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
