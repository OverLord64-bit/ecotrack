package com.ecotrack.report.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTest {

    private static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");

    private static Report report() {
        return new Report("Amira", "dump", WasteCategory.BULKY, 36.8, 10.18, "rue X", T0);
    }

    @Test
    void startsSubmittedWithoutRecord() {
        Report r = report();
        assertThat(r.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(r.getTransmissionStatus()).isNull();
    }

    @Test
    void resolvingCreatesARecordToTransmit() {
        Report r = report();
        r.moveTo(ReportStatus.PLANNED, T0);
        r.moveTo(ReportStatus.RESOLVED, T0.plusSeconds(60));
        assertThat(r.getResolvedAt()).isEqualTo(T0.plusSeconds(60));
        assertThat(r.getTransmissionStatus()).isEqualTo(TransmissionStatus.PENDING_TRANSMISSION);
    }

    @Test
    void recordLifecycle() {
        Report r = report();
        r.transmissionPending("down", T0);
        assertThat(r.getTransmissionStatus()).isEqualTo(TransmissionStatus.PENDING_TRANSMISSION);
        assertThat(r.getTransmissionAttempts()).isEqualTo(1);
        r.transmissionSent("MUN-2026-00001", T0);
        assertThat(r.getTransmissionStatus()).isEqualTo(TransmissionStatus.TRANSMITTED);
        assertThat(r.getTransmissionReference()).isEqualTo("MUN-2026-00001");
        assertThat(r.getTransmissionError()).isNull();
    }
}
