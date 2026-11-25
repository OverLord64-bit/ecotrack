package com.ecotrack.collection.logic;

import com.ecotrack.collection.domain.Priority;

import java.time.Instant;
import java.util.UUID;

/** Transport-independent input of the planning use case (the gRPC layer converts to this). */
public record PlanCommand(UUID requestId, UUID reportId, String address, Instant scheduledFor,
                          Priority priority) {
}
