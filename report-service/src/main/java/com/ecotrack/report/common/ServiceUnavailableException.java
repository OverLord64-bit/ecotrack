package com.ecotrack.report.common;

/** A downstream service is down, too slow, or its circuit breaker is open: HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
