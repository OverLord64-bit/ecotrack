package com.ecotrack.report.domain;

import com.ecotrack.report.common.InvalidTransitionException;

import java.util.Map;
import java.util.Set;

/**
 * Report state machine: the backend refuses every transition that is not listed.
 *
 *   SUBMITTED -> PLANNED                    validated and planned over gRPC
 *   SUBMITTED -> PENDING_PLANNING    validated, but collection-service was unavailable (fallback)
 *   PENDING_PLANNING -> PLANNED  planned later by the retry job
 *   PLANNED -> IN_PROGRESS -> RESOLVED        driven by the gRPC status stream
 *   SUBMITTED | PENDING_PLANNING -> REJECTED   by an agent, with a comment
 */
public enum ReportStatus {
    SUBMITTED, PENDING_PLANNING, PLANNED, IN_PROGRESS, RESOLVED, REJECTED;

    private static final Map<ReportStatus, Set<ReportStatus>> ALLOWED = Map.of(
            SUBMITTED, Set.of(PLANNED, PENDING_PLANNING, REJECTED),
            PENDING_PLANNING, Set.of(PLANNED, REJECTED),
            PLANNED, Set.of(IN_PROGRESS, RESOLVED),
            IN_PROGRESS, Set.of(RESOLVED),
            RESOLVED, Set.of(),
            REJECTED, Set.of());

    public boolean canMoveTo(ReportStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    public ReportStatus moveTo(ReportStatus next) {
        if (!canMoveTo(next)) {
            throw new InvalidTransitionException(this, next);
        }
        return next;
    }
}
