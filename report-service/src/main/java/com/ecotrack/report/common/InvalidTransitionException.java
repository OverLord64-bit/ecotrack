package com.ecotrack.report.common;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(Enum<?> from, Enum<?> to) {
        super(from + " cannot move to " + to);
    }
}
