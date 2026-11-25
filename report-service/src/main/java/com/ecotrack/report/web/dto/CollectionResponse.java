package com.ecotrack.report.web.dto;

import com.ecotrack.contracts.collection.v1.CollectionInfo;

import java.time.Instant;

public record CollectionResponse(String id, String reportId, String address, String status, String priority,
                                 Instant scheduledFor, Instant createdAt, Instant updatedAt) {

    public static CollectionResponse from(CollectionInfo c) {
        return new CollectionResponse(c.getId(), c.getReportId(), c.getAddress(),
                c.getStatus().name().substring("COLLECTION_STATUS_".length()),
                c.getPriority().name().substring("PRIORITY_".length()),
                Instant.ofEpochSecond(c.getScheduledFor().getSeconds(), c.getScheduledFor().getNanos()),
                Instant.ofEpochSecond(c.getCreatedAt().getSeconds(), c.getCreatedAt().getNanos()),
                Instant.ofEpochSecond(c.getUpdatedAt().getSeconds(), c.getUpdatedAt().getNanos()));
    }
}
