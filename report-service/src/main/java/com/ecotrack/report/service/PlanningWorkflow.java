package com.ecotrack.report.service;

import com.ecotrack.contracts.collection.v1.CollectionInfo;
import com.ecotrack.contracts.collection.v1.PlanCollectionRequest;
import com.ecotrack.contracts.collection.v1.Priority;
import com.ecotrack.report.collection.CollectionWatcher;
import com.ecotrack.report.collection.CollectionClient;
import com.ecotrack.report.common.ServiceUnavailableException;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.events.SseHub;
import com.ecotrack.report.web.dto.PriorityLevel;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Validation of a report by an agent = planning of a collection over gRPC.
 *  - gRPC answers          -> report PLANNED, status stream followed
 *  - circuit open / outage -> fallback: report PENDING_PLANNING, retried by a scheduled job
 *  - business refusal      -> propagated as an HTTP error (409, 422...)
 * The gRPC request_id is derived from the report id, so every retry of the same planning is idempotent
 * on the collection-service side (a lost answer cannot create two collections).
 */
@Service
public class PlanningWorkflow {

    private static final Logger log = LoggerFactory.getLogger(PlanningWorkflow.class);

    private final ReportService reports;
    private final CollectionClient collections;
    private final CollectionWatcher watcher;
    private final SseHub hub;
    private final Clock clock;

    public PlanningWorkflow(ReportService reports, CollectionClient collections, CollectionWatcher watcher,
                            SseHub hub, Clock clock) {
        this.reports = reports;
        this.collections = collections;
        this.watcher = watcher;
        this.hub = hub;
        this.clock = clock;
    }

    public Report validate(UUID reportId, Instant scheduledAt, PriorityLevel priority, String actor) {
        Report report = reports.requirePlannable(reportId);
        Instant when = scheduledAt != null ? scheduledAt : clock.instant().plus(Duration.ofDays(1));
        return plan(report, when, priority == null ? PriorityLevel.NORMAL : priority, actor);
    }

    private Report plan(Report report, Instant when, PriorityLevel priority, String actor) {
        var request = PlanCollectionRequest.newBuilder()
                .setRequestId(UUID.nameUUIDFromBytes(("plan:" + report.getId()).getBytes(StandardCharsets.UTF_8)).toString())
                .setReportId(report.getId().toString())
                .setAddress(report.getAddress() == null ? "" : report.getAddress())
                .setScheduledFor(Timestamp.newBuilder().setSeconds(when.getEpochSecond()).setNanos(when.getNano()))
                .setPriority(Priority.valueOf("PRIORITY_" + priority.name()))
                .build();
        Report result;
        try {
            CollectionInfo planned = collections.plan(request);
            result = reports.markPlanned(report.getId(), UUID.fromString(planned.getId()), actor);
            watcher.watch(UUID.fromString(planned.getId()));
        } catch (ServiceUnavailableException e) {
            log.warn("planning of report {} postponed: {}", report.getId(), e.getMessage());
            result = reports.markAwaitingPlanning(report.getId(), "collection-service unavailable: " + e.getMessage());
        }
        hub.publish(result);
        return result;
    }

    @Scheduled(fixedDelayString = "${ecotrack.planning.retry-interval-ms:15000}",
            initialDelayString = "${ecotrack.planning.retry-interval-ms:15000}")
    public void retryWaiting() {
        for (Report report : reports.awaitingPlanning()) {
            try {
                plan(report, clock.instant().plus(Duration.ofDays(1)), PriorityLevel.NORMAL, "retry-job");
            } catch (RuntimeException e) {
                log.warn("retry of planning for report {} failed: {}", report.getId(), e.toString());
            }
        }
    }
}
