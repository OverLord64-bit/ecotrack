package com.ecotrack.report.soap;

/** The municipal system could not be reached in time (down, slow, or the circuit breaker is open). */
public class MunicipalUnavailableException extends RuntimeException {
    public MunicipalUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
