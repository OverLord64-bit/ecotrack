package com.ecotrack.report.service;

import com.ecotrack.contracts.collection.v1.CollectionInfo;
import com.ecotrack.report.collection.CollectionClient;
import com.ecotrack.report.collection.CollectionRejectedException;
import com.ecotrack.report.collection.CollectionWatcher;
import com.ecotrack.report.common.ServiceUnavailableException;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.domain.WasteCategory;
import com.ecotrack.report.events.SseHub;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The fallback behaviours of the gRPC call: PENDING_PLANNING when collection-service is down. */
class PlanningWorkflowTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private ReportService reports;
    private CollectionClient collections;
    private CollectionWatcher watcher;
    private PlanningWorkflow workflow;
    private Report report;

    @BeforeEach
    void setUp() {
        reports = mock(ReportService.class);
        collections = mock(CollectionClient.class);
        watcher = mock(CollectionWatcher.class);
        workflow = new PlanningWorkflow(reports, collections, watcher, mock(SseHub.class), Clock.fixed(NOW, ZoneOffset.UTC));
        report = new Report("A", "dump", WasteCategory.BULKY, 36.8, 10.18, "rue", NOW);
        when(reports.requirePlannable(report.getId())).thenReturn(report);
    }

    @Test
    void successPlansAndStartsFollowingTheCollection() {
        UUID collection = UUID.randomUUID();
        when(collections.plan(any())).thenReturn(CollectionInfo.newBuilder().setId(collection.toString()).build());
        when(reports.markPlanned(eq(report.getId()), eq(collection), any())).thenReturn(report);

        workflow.validate(report.getId(), null, null, "agent");

        verify(reports).markPlanned(report.getId(), collection, "agent");
        verify(watcher).watch(collection);
    }

    @Test
    void outageFallsBackToEnAttentePlanification() {
        when(collections.plan(any())).thenThrow(new ServiceUnavailableException("down", null));
        when(reports.markAwaitingPlanning(eq(report.getId()), any())).thenReturn(report);

        workflow.validate(report.getId(), null, null, "agent");   // no exception: the fallback absorbed it

        verify(reports).markAwaitingPlanning(eq(report.getId()), any());
        verify(reports, never()).markPlanned(any(), any(), any());
        verify(watcher, never()).watch(any());
    }

    @Test
    void businessRefusalIsPropagatedAndNothingChanges() {
        when(collections.plan(any())).thenThrow(new CollectionRejectedException(Status.Code.ABORTED, "already planned"));

        assertThatThrownBy(() -> workflow.validate(report.getId(), null, null, "agent"))
                .isInstanceOf(CollectionRejectedException.class);
        verify(reports, never()).markAwaitingPlanning(any(), any());
    }

    @Test
    void retryJobReplansWaitingReports() {
        UUID collection = UUID.randomUUID();
        report.moveTo(ReportStatus.PENDING_PLANNING, NOW);
        when(reports.awaitingPlanning()).thenReturn(java.util.List.of(report));
        when(collections.plan(any())).thenReturn(CollectionInfo.newBuilder().setId(collection.toString()).build());
        when(reports.markPlanned(any(), any(), any())).thenReturn(report);

        workflow.retryWaiting();

        verify(reports).markPlanned(report.getId(), collection, "retry-job");
    }

    @Test
    void theGrpcRequestIdIsStableForTheSameReport() {
        var captured = org.mockito.ArgumentCaptor.forClass(com.ecotrack.contracts.collection.v1.PlanCollectionRequest.class);
        when(collections.plan(captured.capture())).thenThrow(new ServiceUnavailableException("down", null));
        when(reports.markAwaitingPlanning(any(), any())).thenReturn(report);

        workflow.validate(report.getId(), null, null, "agent");
        workflow.validate(report.getId(), null, null, "agent");

        assertThat(captured.getAllValues().get(0).getRequestId()).isEqualTo(captured.getAllValues().get(1).getRequestId());
    }
}
