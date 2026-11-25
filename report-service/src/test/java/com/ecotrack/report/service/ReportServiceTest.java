package com.ecotrack.report.service;

import com.ecotrack.report.common.BusinessRuleException;
import com.ecotrack.report.common.InvalidInputException;
import com.ecotrack.report.common.NotFoundException;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.domain.WasteCategory;
import com.ecotrack.report.repo.ReportRepository;
import com.ecotrack.report.repo.ReportStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private ReportRepository reports;
    private ReportStatusHistoryRepository history;
    private ReportService service;

    @BeforeEach
    void setUp() {
        reports = mock(ReportRepository.class);
        history = mock(ReportStatusHistoryRepository.class);
        service = new ReportService(reports, history, Clock.fixed(NOW, ZoneOffset.UTC));
        when(reports.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Report stored(ReportStatus status) {
        Report r = new Report("A", "dump", WasteCategory.BULKY, 36.8, 10.18, "rue", NOW);
        if (status == ReportStatus.PENDING_PLANNING) r.moveTo(status, NOW);
        if (status == ReportStatus.PLANNED) r.moveTo(status, NOW);
        when(reports.findById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void createStartsSubmittedAndWritesHistory() {
        Report r = service.create("A", "dump", WasteCategory.BULKY, 36.8, 10.18, "rue");
        assertThat(r.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        verify(history).save(any());
    }

    @Test
    void unknownReportIsNotFound() {
        when(reports.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectNeedsAComment() {
        Report r = stored(ReportStatus.SUBMITTED);
        assertThatThrownBy(() -> service.reject(r.getId(), " ", "agent")).isInstanceOf(InvalidInputException.class);
        assertThat(service.reject(r.getId(), "duplicate", "agent").getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void plannedReportCannotBeRejected() {
        Report r = stored(ReportStatus.PLANNED);
        assertThatThrownBy(() -> service.reject(r.getId(), "too late", "agent"))
                .hasMessageContaining("PLANNED");
    }

    @Test
    void onlySoumisOrWaitingReportsArePlannable() {
        assertThat(service.requirePlannable(stored(ReportStatus.SUBMITTED).getId())).isNotNull();
        assertThat(service.requirePlannable(stored(ReportStatus.PENDING_PLANNING).getId())).isNotNull();
        UUID planned = stored(ReportStatus.PLANNED).getId();
        assertThatThrownBy(() -> service.requirePlannable(planned)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markAwaitingPlanningIsIdempotent() {
        Report r = stored(ReportStatus.PENDING_PLANNING);
        assertThat(service.markAwaitingPlanning(r.getId(), "down").getStatus())
                .isEqualTo(ReportStatus.PENDING_PLANNING);
    }

    @Test
    void collectionEventsAreAppliedOnceAndIgnoredWhenIllegal() {
        Report r = stored(ReportStatus.SUBMITTED);
        UUID collection = UUID.randomUUID();
        service.markPlanned(r.getId(), collection, "agent");
        when(reports.findByCollectionId(collection)).thenReturn(Optional.of(r));

        service.applyCollectionStatus(collection, ReportStatus.IN_PROGRESS);
        service.applyCollectionStatus(collection, ReportStatus.IN_PROGRESS);   // duplicate event: no error
        assertThat(r.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);

        service.applyCollectionStatus(collection, ReportStatus.PLANNED);   // backwards: ignored
        assertThat(r.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);

        service.applyCollectionStatus(collection, ReportStatus.RESOLVED);
        assertThat(r.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(r.getTransmissionStatus()).isNotNull();
    }
}
