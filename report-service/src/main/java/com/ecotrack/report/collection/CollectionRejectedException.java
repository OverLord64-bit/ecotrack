package com.ecotrack.report.collection;

import io.grpc.Status;
import org.springframework.http.HttpStatus;

/** collection-service answered with a business error (not an outage): mapped to an HTTP status. */
public class CollectionRejectedException extends RuntimeException {

    private final Status.Code grpcCode;

    public CollectionRejectedException(Status.Code grpcCode, String message) {
        super(message == null ? grpcCode.name() : message);
        this.grpcCode = grpcCode;
    }

    public Status.Code getGrpcCode() {
        return grpcCode;
    }

    public HttpStatus httpStatus() {
        return switch (grpcCode) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT -> HttpStatus.UNPROCESSABLE_ENTITY;
            case FAILED_PRECONDITION, ABORTED, ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
