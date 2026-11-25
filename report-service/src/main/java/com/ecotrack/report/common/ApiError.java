package com.ecotrack.report.common;

import java.time.Instant;
import java.util.List;

/** The single error shape returned by every endpoint. */
public record ApiError(String code, String message, Instant timestamp, List<String> details) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now(), null);
    }

    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(code, message, Instant.now(), details);
    }
}
