package com.ecotrack.report.service;

import com.ecotrack.contracts.collection.v1.CollectionStatus;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.events.SseHub;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Reacts to a status received on the gRPC stream: updates the report, tells browsers, sends the record. */
@Service
public class CollectionStatusHandler {

    private final ReportService reports;
    private final RecordTransmitter transmitter;
    private final SseHub hub;

    public CollectionStatusHandler(ReportService reports, RecordTransmitter transmitter, SseHub hub) {
        this.reports = reports;
        this.transmitter = transmitter;
        this.hub = hub;
    }

    public void onStatus(UUID collectionId, CollectionStatus status) {
        ReportStatus target = switch (status) {
            case COLLECTION_STATUS_IN_PROGRESS -> ReportStatus.IN_PROGRESS;
            case COLLECTION_STATUS_COMPLETED -> ReportStatus.RESOLVED;
            default -> null;
        };
        if (target == null) {
            return;
        }
        reports.applyCollectionStatus(collectionId, target).ifPresent(report -> {
            hub.publish(report);
            if (report.getStatus() == ReportStatus.RESOLVED) {
                transmitter.transmitAsync(report.getId());
            }
        });
    }
}
