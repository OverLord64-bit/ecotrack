package com.ecotrack.collection.grpc;

import com.ecotrack.collection.domain.CollectionJob;
import com.ecotrack.collection.domain.InvalidRequestException;
import com.ecotrack.collection.domain.Priority;
import com.ecotrack.collection.logic.PlanCommand;
import com.ecotrack.contracts.collection.v1.CollectionStatus;
import com.ecotrack.contracts.collection.v1.PlanCollectionRequest;
import com.ecotrack.contracts.collection.v1.CollectionStatusEvent;
import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.util.UUID;

/** Conversions between the wire contract (protobuf) and the domain. */
public final class GrpcMapper {

    private GrpcMapper() {
    }

    static UUID uuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(field + " is not a valid UUID");
        }
    }

    static PlanCommand toCommand(PlanCollectionRequest r) {
        Priority priority = switch (r.getPriority()) {
            case PRIORITY_LOW -> Priority.LOW;
            case PRIORITY_HIGH -> Priority.HIGH;
            default -> Priority.NORMAL;
        };
        Instant when = r.hasScheduledFor()
                ? Instant.ofEpochSecond(r.getScheduledFor().getSeconds(), r.getScheduledFor().getNanos()) : null;
        return new PlanCommand(uuid(r.getRequestId(), "request_id"), uuid(r.getReportId(), "report_id"),
                r.getAddress().isEmpty() ? null : r.getAddress(), when, priority);
    }

    static com.ecotrack.contracts.collection.v1.CollectionInfo toProto(CollectionJob c) {
        return com.ecotrack.contracts.collection.v1.CollectionInfo.newBuilder()
                .setId(c.getId().toString())
                .setReportId(c.getReportId().toString())
                .setAddress(c.getAddress() == null ? "" : c.getAddress())
                .setStatus(status(c))
                .setPriority(com.ecotrack.contracts.collection.v1.Priority.valueOf("PRIORITY_" + c.getPriority().name()))
                .setScheduledFor(ts(c.getScheduledFor()))
                .setCreatedAt(ts(c.getCreatedAt()))
                .setUpdatedAt(ts(c.getUpdatedAt()))
                .build();
    }

    public static CollectionStatusEvent event(CollectionJob c) {
        return CollectionStatusEvent.newBuilder()
                .setCollectionId(c.getId().toString())
                .setReportId(c.getReportId().toString())
                .setStatus(status(c))
                .setOccurredAt(ts(c.getUpdatedAt()))
                .build();
    }

    private static CollectionStatus status(CollectionJob c) {
        return CollectionStatus.valueOf("COLLECTION_STATUS_" + c.getStatus().name());
    }

    private static Timestamp ts(Instant i) {
        return Timestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();
    }
}
