package com.ecotrack.report.service;

import com.ecotrack.contracts.municipal.SubmitCollectionRecordRequest;
import com.ecotrack.report.domain.TransmissionStatus;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.events.SseHub;
import com.ecotrack.report.soap.MunicipalClient;
import com.ecotrack.report.soap.MunicipalUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.client.SoapFaultClientException;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Sends the record of a resolved report to the municipal system over SOAP.
 * No database transaction is open during the SOAP call. Sending twice is harmless: the municipal system is
 * idempotent on recordId (= collection id), so the immediate attempt and the scheduled retry may overlap.
 */
@Service
public class RecordTransmitter {

    private static final Logger log = LoggerFactory.getLogger(RecordTransmitter.class);

    private final ReportService reports;
    private final MunicipalClient municipal;
    private final SseHub hub;

    public RecordTransmitter(ReportService reports, MunicipalClient municipal, SseHub hub) {
        this.reports = reports;
        this.municipal = municipal;
        this.hub = hub;
    }

    @Async
    public void transmitAsync(UUID reportId) {
        transmit(reportId);
    }

    public Report transmit(UUID reportId) {
        Report report = reports.get(reportId);
        if (report.getTransmissionStatus() != TransmissionStatus.PENDING_TRANSMISSION) {
            return report;
        }
        Report updated;
        try {
            var response = municipal.submit(toRequest(report));
            updated = reports.transmissionSent(reportId, response.getMunicipalReference());
            log.info("record of report {} accepted: {} (already received: {})", reportId,
                    response.getMunicipalReference(), response.isDuplicate());
        } catch (MunicipalUnavailableException e) {
            updated = reports.transmissionPending(reportId, e.getMessage());   // fallback: PENDING_TRANSMISSION, retried by the job
        } catch (SoapFaultClientException fault) {
            log.warn("record of report {} refused: {}", reportId, fault.getFaultStringOrReason());
            updated = reports.transmissionRefused(reportId, fault.getFaultStringOrReason());
        }
        hub.publish(updated);
        return updated;
    }

    @Scheduled(fixedDelayString = "${ecotrack.transmission.retry-interval-ms:10000}",
            initialDelayString = "${ecotrack.transmission.retry-interval-ms:10000}")
    public void retryPending() {
        for (Report report : reports.transmissionsToSend()) {
            try {
                transmit(report.getId());
            } catch (RuntimeException e) {
                log.warn("retry of record {} failed: {}", report.getId(), e.toString());
            }
        }
    }

    private static SubmitCollectionRecordRequest toRequest(Report report) {
        try {
            var request = new SubmitCollectionRecordRequest();
            request.setRecordId(report.getCollectionId().toString());
            request.setCollectionId(report.getCollectionId().toString());
            request.setReportId(report.getId().toString());
            request.setCompletedAt(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(GregorianCalendar.from(report.getResolvedAt().atZone(ZoneOffset.UTC))));
            request.setAddress(report.getAddress());
            return request;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
