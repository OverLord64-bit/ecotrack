package com.ecotrack.municipal.records;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "municipal_records")
public class MunicipalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true)
    private String reference;

    /** Idempotency key: the EcoTrack collection id. */
    @Column(name = "record_id", nullable = false, unique = true)
    private UUID recordId;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "collection_id", nullable = false)
    private UUID collectionId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    private String address;
    private String notes;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected MunicipalRecord() {
    }

    public MunicipalRecord(String reference, UUID recordId, UUID reportId, UUID collectionId,
                   Instant completedAt, String address, String notes) {
        this.reference = reference;
        this.recordId = recordId;
        this.reportId = reportId;
        this.collectionId = collectionId;
        this.completedAt = completedAt;
        this.address = address;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public UUID getRecordId() { return recordId; }
    public UUID getReportId() { return reportId; }
    public UUID getCollectionId() { return collectionId; }
    public Instant getCompletedAt() { return completedAt; }
    public String getAddress() { return address; }
    public String getNotes() { return notes; }
    public Instant getReceivedAt() { return receivedAt; }
}
