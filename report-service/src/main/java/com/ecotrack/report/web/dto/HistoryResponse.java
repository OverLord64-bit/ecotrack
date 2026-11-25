package com.ecotrack.report.web.dto;

import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.domain.ReportStatusHistory;

import java.time.Instant;

public record HistoryResponse(ReportStatus oldStatus, ReportStatus newStatus, String changedBy,
                              Instant changedAt, String comment) {

    public static HistoryResponse from(ReportStatusHistory h) {
        return new HistoryResponse(h.getOldStatus(), h.getNewStatus(), h.getChangedBy(), h.getChangedAt(), h.getComment());
    }
}
