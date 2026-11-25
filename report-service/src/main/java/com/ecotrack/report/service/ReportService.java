package com.ecotrack.report.service;

import com.ecotrack.report.common.BusinessRuleException;
import com.ecotrack.report.common.InvalidInputException;
import com.ecotrack.report.common.NotFoundException;
import com.ecotrack.report.domain.TransmissionStatus;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.domain.ReportStatusHistory;
import com.ecotrack.report.domain.WasteCategory;
import com.ecotrack.report.repo.ReportRepository;
import com.ecotrack.report.repo.ReportStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final ReportStatusHistoryRepository history;
    private final Clock clock;

    public ReportService(ReportRepository reports, ReportStatusHistoryRepository history, Clock clock) {
        this.reports = reports;
        this.history = history;
        this.clock = clock;
    }

    @Transactional
    public Report create(String reporterName, String description, WasteCategory category,
                         double latitude, double longitude, String address) {
        Report report = reports.save(new Report(reporterName, description, category, latitude, longitude, address, clock.instant()));
        history.save(new ReportStatusHistory(report.getId(), null, ReportStatus.SUBMITTED,
                reporterName == null ? "citizen" : reporterName, clock.instant(), "Report submitted"));
        return report;
    }

    @Transactional(readOnly = true)
    public Report get(UUID id) {
        return reports.findById(id).orElseThrow(() -> new NotFoundException("report " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<Report> list(ReportStatus status, WasteCategory category, Pageable pageable) {
        Specification<Report> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("wasteCategory"), category));
        }
        return reports.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<ReportStatusHistory> history(UUID id) {
        get(id);
        return history.findByReportIdOrderByChangedAtAsc(id);
    }

    /** An agent rejects a report; a comment is required. Only SUBMITTED and PENDING_PLANNING can be rejected. */
    @Transactional
    public Report reject(UUID id, String comment, String actor) {
        if (comment == null || comment.isBlank()) {
            throw new InvalidInputException("a comment is required to reject a report");
        }
        return transition(get(id), ReportStatus.REJECTED, actor, comment);
    }

    /** First step of validation: the report must still be waiting for a collection. */
    @Transactional(readOnly = true)
    public Report requirePlannable(UUID id) {
        Report report = get(id);
        if (report.getStatus() != ReportStatus.SUBMITTED && report.getStatus() != ReportStatus.PENDING_PLANNING) {
            throw new BusinessRuleException("REPORT_NOT_PLANNABLE",
                    "report " + id + " is " + report.getStatus() + ", it must be SUBMITTED or PENDING_PLANNING");
        }
        return report;
    }

    @Transactional
    public Report markPlanned(UUID id, UUID collectionId, String actor) {
        Report report = get(id);
        report.assignTo(collectionId);
        return transition(report, ReportStatus.PLANNED, actor, "Planned in collection " + collectionId);
    }

    /** Fallback of the circuit breaker: the report is validated but no collection could be planned yet. */
    @Transactional
    public Report markAwaitingPlanning(UUID id, String reason) {
        Report report = get(id);
        if (report.getStatus() == ReportStatus.PENDING_PLANNING) {
            return report;
        }
        return transition(report, ReportStatus.PENDING_PLANNING, "report-service", reason);
    }

    @Transactional(readOnly = true)
    public List<Report> awaitingPlanning() {
        return reports.findByStatus(ReportStatus.PENDING_PLANNING);
    }

    @Transactional(readOnly = true)
    public List<Report> followedByACollection() {
        return reports.findByStatusIn(List.of(ReportStatus.PLANNED, ReportStatus.IN_PROGRESS));
    }

    @Transactional(readOnly = true)
    public List<Report> transmissionsToSend() {
        return reports.findByTransmissionStatus(TransmissionStatus.PENDING_TRANSMISSION);
    }

    /** Called from the gRPC stream. Safe to receive twice or out of order: illegal moves are ignored. */
    @Transactional
    public Optional<Report> applyCollectionStatus(UUID collectionId, ReportStatus target) {
        return reports.findByCollectionId(collectionId).map(report -> {
            if (report.getStatus() != target && report.getStatus().canMoveTo(target)) {
                return transition(report, target, "collection-service", "Collection " + collectionId + " is now " + target);
            }
            return report;
        });
    }

    @Transactional
    public Report attachPhoto(UUID id, String filename) {
        Report report = get(id);
        report.attachPhoto(filename);
        return reports.save(report);
    }

    @Transactional
    public Report transmissionSent(UUID id, String reference) {
        Report report = get(id);
        report.transmissionSent(reference, clock.instant());
        return reports.save(report);
    }

    @Transactional
    public Report transmissionPending(UUID id, String error) {
        Report report = get(id);
        report.transmissionPending(error, clock.instant());
        return reports.save(report);
    }

    @Transactional
    public Report transmissionRefused(UUID id, String error) {
        Report report = get(id);
        report.transmissionRefused(error, clock.instant());
        return reports.save(report);
    }

    private Report transition(Report report, ReportStatus next, String actor, String comment) {
        ReportStatus previous = report.getStatus();
        report.moveTo(next, clock.instant());
        reports.save(report);
        history.save(new ReportStatusHistory(report.getId(), previous, next, actor, clock.instant(), comment));
        return report;
    }
}
