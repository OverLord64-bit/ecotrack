package com.ecotrack.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A citizen's report . */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    private UUID id;
    private String reporterName;
    private String description;

    @Enumerated(EnumType.STRING)
    private WasteCategory wasteCategory;

    private double latitude;
    private double longitude;
    private String address;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(name = "collection_id")
    private UUID collectionId;

    @Column(name = "photo_filename")
    private String photoFilename;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_status")
    private TransmissionStatus transmissionStatus;

    @Column(name = "transmission_reference")
    private String transmissionReference;

    @Column(name = "transmission_attempts", nullable = false)
    private int transmissionAttempts;

    @Column(name = "transmission_error")
    private String transmissionError;

    private Instant createdAt;
    private Instant updatedAt;

    protected Report() {
    }

    public Report(String reporterName, String description, WasteCategory wasteCategory,
                  double latitude, double longitude, String address, Instant now) {
        this.id = UUID.randomUUID();
        this.reporterName = reporterName;
        this.description = description;
        this.wasteCategory = wasteCategory;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = ReportStatus.SUBMITTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void moveTo(ReportStatus next, Instant now) {
        this.status = status.moveTo(next);
        this.updatedAt = now;
        if (next == ReportStatus.RESOLVED) {
            this.resolvedAt = now;
            this.transmissionStatus = TransmissionStatus.PENDING_TRANSMISSION;
        }
    }

    public void assignTo(UUID collectionId) {
        this.collectionId = collectionId;
    }

    public void attachPhoto(String filename) {
        this.photoFilename = filename;
    }

    public void transmissionSent(String reference, Instant now) {
        this.transmissionStatus = TransmissionStatus.TRANSMITTED;
        this.transmissionReference = reference;
        this.transmissionAttempts++;
        this.transmissionError = null;
        this.updatedAt = now;
    }

    public void transmissionPending(String error, Instant now) {
        this.transmissionStatus = TransmissionStatus.PENDING_TRANSMISSION;
        this.transmissionAttempts++;
        this.transmissionError = error;
        this.updatedAt = now;
    }

    public void transmissionRefused(String error, Instant now) {
        this.transmissionStatus = TransmissionStatus.REFUSED;
        this.transmissionAttempts++;
        this.transmissionError = error;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getReporterName() { return reporterName; }
    public String getDescription() { return description; }
    public WasteCategory getWasteCategory() { return wasteCategory; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public ReportStatus getStatus() { return status; }
    public UUID getCollectionId() { return collectionId; }
    public String getPhotoFilename() { return photoFilename; }
    public Instant getResolvedAt() { return resolvedAt; }
    public TransmissionStatus getTransmissionStatus() { return transmissionStatus; }
    public String getTransmissionReference() { return transmissionReference; }
    public int getTransmissionAttempts() { return transmissionAttempts; }
    public String getTransmissionError() { return transmissionError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
