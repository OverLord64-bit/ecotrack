package com.ecotrack.report.web.dto;

import jakarta.validation.constraints.Future;

import java.time.Instant;

/** Optional parameters of a validation: defaults are tomorrow and NORMAL. */
public record ValidateBody(@Future Instant scheduledAt, PriorityLevel priority) {
}
