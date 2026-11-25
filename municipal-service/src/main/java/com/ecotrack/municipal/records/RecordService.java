package com.ecotrack.municipal.records;

import com.ecotrack.contracts.municipal.GetRecordStatusResponse;
import com.ecotrack.contracts.municipal.SubmitCollectionRecordRequest;
import com.ecotrack.contracts.municipal.SubmitCollectionRecordResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.UUID;

@Service
public class RecordService {

    private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(5);

    private final MunicipalRecordRepository repository;
    private final Clock clock;

    public RecordService(MunicipalRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Idempotent: recordId is the key. A repeated transmission returns the existing record's reference with
     * duplicate=true instead of inserting again. Deliberately not @Transactional so a unique-constraint race
     * between two concurrent transmissions can be caught and resolved here.
     */
    public SubmitCollectionRecordResponse submit(SubmitCollectionRecordRequest request) {
        UUID recordId = UUID.fromString(request.getRecordId());
        Instant completedAt = request.getCompletedAt().toGregorianCalendar().toInstant();
        if (completedAt.isAfter(clock.instant().plus(ALLOWED_CLOCK_SKEW))) {
            throw new InvalidRecordException("completedAt is in the future");
        }

        var existing = repository.findByRecordId(recordId);
        if (existing.isPresent()) {
            return response(existing.get(), true);
        }

        long sequence = repository.nextSequence();
        String reference = String.format("MUN-%d-%05d", clock.instant().atZone(ZoneOffset.UTC).getYear(), sequence);
        MunicipalRecord record = new MunicipalRecord(reference, recordId,
                UUID.fromString(request.getReportId()), UUID.fromString(request.getCollectionId()),
                completedAt, request.getAddress(), request.getNotes());
        try {
            return response(repository.save(record), false);
        } catch (DataIntegrityViolationException race) {
            return repository.findByRecordId(recordId)
                    .map(d -> response(d, true))
                    .orElseThrow(() -> race);
        }
    }

    public GetRecordStatusResponse status(String recordId) {
        GetRecordStatusResponse response = new GetRecordStatusResponse();
        var found = repository.findByRecordId(UUID.fromString(recordId));
        response.setFound(found.isPresent());
        found.ifPresent(d -> {
            response.setStatus("RECEIVED");
            response.setMunicipalReference(d.getReference());
        });
        return response;
    }

    private SubmitCollectionRecordResponse response(MunicipalRecord record, boolean duplicate) {
        SubmitCollectionRecordResponse response = new SubmitCollectionRecordResponse();
        response.setAccepted(true);
        response.setMunicipalReference(record.getReference());
        response.setReceivedAt(toXml(clock.instant()));
        response.setDuplicate(duplicate);
        return response;
    }

    private static XMLGregorianCalendar toXml(Instant instant) {
        try {
            return DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(GregorianCalendar.from(instant.atZone(ZoneOffset.UTC)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
