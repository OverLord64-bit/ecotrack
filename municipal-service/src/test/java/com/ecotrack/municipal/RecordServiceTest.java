package com.ecotrack.municipal;

import com.ecotrack.contracts.municipal.SubmitCollectionRecordRequest;
import com.ecotrack.municipal.records.MunicipalRecord;
import com.ecotrack.municipal.records.InvalidRecordException;
import com.ecotrack.municipal.records.MunicipalRecordRepository;
import com.ecotrack.municipal.records.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import javax.xml.datatype.DatatypeFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private MunicipalRecordRepository repository;
    private RecordService service;

    @BeforeEach
    void setUp() {
        repository = mock(MunicipalRecordRepository.class);
        service = new RecordService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void storesANewRecordAndReturnsItsReference() {
        when(repository.findByRecordId(any())).thenReturn(Optional.empty());
        when(repository.nextSequence()).thenReturn(42L);
        when(repository.save(any(MunicipalRecord.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.submit(request(UUID.randomUUID(), NOW.minusSeconds(60)));

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.isDuplicate()).isFalse();
        assertThat(response.getMunicipalReference()).isEqualTo("MUN-2026-00042");
    }

    @Test
    void sameRecordTwiceIsADuplicateAndIsNotStoredAgain() {
        UUID id = UUID.randomUUID();
        MunicipalRecord existing = new MunicipalRecord("MUN-2026-00007", id, UUID.randomUUID(), UUID.randomUUID(), NOW, null, null);
        when(repository.findByRecordId(id)).thenReturn(Optional.of(existing));

        var response = service.submit(request(id, NOW.minusSeconds(60)));

        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getMunicipalReference()).isEqualTo("MUN-2026-00007");
        verify(repository, never()).save(any());
    }

    @Test
    void concurrentInsertLosesTheRaceAndReturnsTheWinnersRecord() {
        UUID id = UUID.randomUUID();
        MunicipalRecord winner = new MunicipalRecord("MUN-2026-00009", id, UUID.randomUUID(), UUID.randomUUID(), NOW, null, null);
        when(repository.findByRecordId(id)).thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.nextSequence()).thenReturn(10L);
        when(repository.save(any(MunicipalRecord.class))).thenThrow(new DataIntegrityViolationException("unique"));

        var response = service.submit(request(id, NOW.minusSeconds(60)));

        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getMunicipalReference()).isEqualTo("MUN-2026-00009");
    }

    @Test
    void rejectsACompletionDateInTheFuture() {
        assertThatThrownBy(() -> service.submit(request(UUID.randomUUID(), NOW.plusSeconds(3600))))
                .isInstanceOf(InvalidRecordException.class);
    }

    @Test
    void statusOfUnknownRecordIsNotFound() {
        when(repository.findByRecordId(any())).thenReturn(Optional.empty());
        var response = service.status(UUID.randomUUID().toString());
        assertThat(response.isFound()).isFalse();
        assertThat(response.getMunicipalReference()).isNull();
    }

    private static SubmitCollectionRecordRequest request(UUID recordId, Instant completedAt) {
        try {
            var r = new SubmitCollectionRecordRequest();
            r.setRecordId(recordId.toString());
            r.setReportId(UUID.randomUUID().toString());
            r.setCollectionId(recordId.toString());
            r.setCompletedAt(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(GregorianCalendar.from(completedAt.atZone(ZoneOffset.UTC))));
            return r;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
