package com.ecotrack.report.web.dto;

import com.ecotrack.report.domain.TransmissionStatus;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.domain.WasteCategory;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(UUID id, String reporterName, String description, WasteCategory wasteCategory,
                             double latitude, double longitude, String address, ReportStatus status,
                             UUID collectionId, String photoUrl, TransmissionStatus transmissionStatus,
                             String transmissionReference, int transmissionAttempts, String transmissionError,
                             Instant resolvedAt, Instant createdAt, Instant updatedAt) {

    public static ReportResponse from(Report r) {
        return new ReportResponse(r.getId(), r.getReporterName(), r.getDescription(), r.getWasteCategory(),
                r.getLatitude(), r.getLongitude(), r.getAddress(), r.getStatus(), r.getCollectionId(),
                r.getPhotoFilename() == null ? null : "/api/v1/reports/" + r.getId() + "/photo",
                r.getTransmissionStatus(), r.getTransmissionReference(), r.getTransmissionAttempts(), r.getTransmissionError(),
                r.getResolvedAt(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
