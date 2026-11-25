package com.ecotrack.collection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collections")
public class CollectionJob {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionState status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CollectionJob() {
    }

    public CollectionJob(UUID requestId, UUID reportId, String address, Priority priority,
                    Instant scheduledFor, Instant now) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
        this.reportId = reportId;
        this.address = address;
        this.priority = priority;
        this.scheduledFor = scheduledFor;
        this.status = CollectionState.PLANNED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Moves to the next state; a finished collection never changes again. */
    public void advance(Instant now) {
        if (status.isFinal()) {
            throw new PreconditionFailedException("collection " + id + " is already finished");
        }
        status = status.next();
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public UUID getReportId() { return reportId; }
    public String getAddress() { return address; }
    public CollectionState getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
