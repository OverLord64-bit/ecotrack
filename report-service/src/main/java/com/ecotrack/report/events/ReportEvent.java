package com.ecotrack.report.events;

import com.ecotrack.report.domain.TransmissionStatus;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;

import java.time.Instant;
import java.util.UUID;

/** Payload of the Server-Sent Events stream: one message each time a report changes. */
public record ReportEvent(UUID reportId, ReportStatus status, UUID collectionId, TransmissionStatus transmissionStatus,
                          String transmissionReference, Instant at) {

    public static ReportEvent from(Report r) {
        return new ReportEvent(r.getId(), r.getStatus(), r.getCollectionId(), r.getTransmissionStatus(),
                r.getTransmissionReference(), r.getUpdatedAt());
    }
}
