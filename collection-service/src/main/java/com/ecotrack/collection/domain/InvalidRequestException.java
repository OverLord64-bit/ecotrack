package com.ecotrack.collection.domain;

import io.grpc.Status;

public class InvalidRequestException extends DomainException {
    public InvalidRequestException(String message) {
        super(message);
    }

    @Override
    public Status.Code code() {
        return Status.Code.INVALID_ARGUMENT;
    }
}
