package com.ecotrack.report.domain;

import com.ecotrack.report.common.InvalidTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The full transition matrix, written independently of the implementation. */
class ReportStatusTest {

    private static final Map<ReportStatus, Set<ReportStatus>> EXPECTED = Map.of(
            ReportStatus.SUBMITTED, EnumSet.of(ReportStatus.PLANNED, ReportStatus.PENDING_PLANNING, ReportStatus.REJECTED),
            ReportStatus.PENDING_PLANNING, EnumSet.of(ReportStatus.PLANNED, ReportStatus.REJECTED),
            ReportStatus.PLANNED, EnumSet.of(ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED),
            ReportStatus.IN_PROGRESS, EnumSet.of(ReportStatus.RESOLVED),
            ReportStatus.RESOLVED, EnumSet.noneOf(ReportStatus.class),
            ReportStatus.REJECTED, EnumSet.noneOf(ReportStatus.class));

    static Stream<ReportStatus[]> allPairs() {
        return Stream.of(ReportStatus.values())
                .flatMap(from -> Stream.of(ReportStatus.values()).map(to -> new ReportStatus[]{from, to}));
    }

    @ParameterizedTest
    @MethodSource("allPairs")
    void everyPairBehavesAsSpecified(ReportStatus from, ReportStatus to) {
        if (EXPECTED.get(from).contains(to)) {
            assertEquals(to, from.moveTo(to));
        } else {
            assertThrows(InvalidTransitionException.class, () -> from.moveTo(to));
        }
    }

    @Test
    void resoluAndRejeteAreTerminal() {
        assertEquals(0, EXPECTED.get(ReportStatus.RESOLVED).size());
        assertEquals(0, EXPECTED.get(ReportStatus.REJECTED).size());
    }
}
