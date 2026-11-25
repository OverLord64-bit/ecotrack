package com.ecotrack.report.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_status_history")
public class ReportStatusHistory {

    @Id
    private UUID id;
    private UUID reportId;

    @Enumerated(EnumType.STRING)
    private ReportStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private ReportStatus newStatus;

    private String changedBy;
    private Instant changedAt;
    private String comment;

    protected ReportStatusHistory() {
    }

    public ReportStatusHistory(UUID reportId, ReportStatus oldStatus, ReportStatus newStatus,
                               String changedBy, Instant changedAt, String comment) {
        this.id = UUID.randomUUID();
        this.reportId = reportId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public UUID getReportId() { return reportId; }
    public ReportStatus getOldStatus() { return oldStatus; }
    public ReportStatus getNewStatus() { return newStatus; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getComment() { return comment; }
}
