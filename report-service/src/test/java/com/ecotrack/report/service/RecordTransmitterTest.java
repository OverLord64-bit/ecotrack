package com.ecotrack.report.service;

import com.ecotrack.contracts.municipal.SubmitCollectionRecordRequest;
import com.ecotrack.contracts.municipal.SubmitCollectionRecordResponse;
import com.ecotrack.report.domain.TransmissionStatus;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.domain.WasteCategory;
import com.ecotrack.report.events.SseHub;
import com.ecotrack.report.soap.MunicipalClient;
import com.ecotrack.report.soap.MunicipalUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Fallback behaviours of the SOAP call: PENDING_TRANSMISSION on outage, REFUSED on SOAP Fault, TRANSMITTED on success. */
class RecordTransmitterTest {

    private ReportService reports;
    private MunicipalClient municipal;
    private RecordTransmitter transmitter;
    private Report report;

    @BeforeEach
    void setUp() {
        reports = mock(ReportService.class);
        municipal = mock(MunicipalClient.class);
        transmitter = new RecordTransmitter(reports, municipal, mock(SseHub.class));
        report = new Report("A", "dump", WasteCategory.BULKY, 36.8, 10.18, "rue", Instant.parse("2026-11-20T10:00:00Z"));
        report.assignTo(UUID.randomUUID());
        report.moveTo(ReportStatus.PLANNED, Instant.now());
        report.moveTo(ReportStatus.RESOLVED, Instant.parse("2026-11-20T11:00:00Z"));
        when(reports.get(report.getId())).thenReturn(report);
        when(reports.transmissionSent(any(), any())).thenReturn(report);
        when(reports.transmissionPending(any(), any())).thenReturn(report);
        when(reports.transmissionRefused(any(), any())).thenReturn(report);
    }

    @Test
    void successMarksTransmis() {
        var response = new SubmitCollectionRecordResponse();
        response.setMunicipalReference("MUN-2026-00001");
        when(municipal.submit(any())).thenReturn(response);

        transmitter.transmit(report.getId());

        verify(reports).transmissionSent(report.getId(), "MUN-2026-00001");
    }

    @Test
    void requestCarriesTheCollectionIdAsIdempotencyKey() {
        var captured = org.mockito.ArgumentCaptor.forClass(SubmitCollectionRecordRequest.class);
        when(municipal.submit(captured.capture())).thenReturn(new SubmitCollectionRecordResponse());

        transmitter.transmit(report.getId());

        org.assertj.core.api.Assertions.assertThat(captured.getValue().getRecordId())
                .isEqualTo(report.getCollectionId().toString());
    }

    @Test
    void outageKeepsTheRecordPendingTransmission() {
        when(municipal.submit(any())).thenThrow(new MunicipalUnavailableException("down", null));

        transmitter.transmit(report.getId());

        verify(reports).transmissionPending(any(), any());
        verify(reports, never()).transmissionSent(any(), any());
    }

    @Test
    void soapFaultMarksRefuseAndIsNotRetried() {
        var fault = mock(SoapFaultClientException.class);
        when(fault.getFaultStringOrReason()).thenReturn("RecordRejected");
        when(municipal.submit(any())).thenThrow(fault);

        transmitter.transmit(report.getId());

        verify(reports).transmissionRefused(report.getId(), "RecordRejected");
    }

    @Test
    void nothingIsSentForAnAlreadyTransmittedRecord() {
        report.transmissionSent("MUN-1", Instant.now());
        transmitter.transmit(report.getId());
        verify(municipal, never()).submit(any());
    }

    @Test
    void retryJobSendsEveryPendingRecord() {
        when(reports.transmissionsToSend()).thenReturn(List.of(report));
        when(municipal.submit(any())).thenReturn(new SubmitCollectionRecordResponse());
        transmitter.retryPending();
        verify(municipal).submit(any());
    }
}
