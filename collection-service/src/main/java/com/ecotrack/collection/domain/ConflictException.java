package com.ecotrack.collection.domain;

import io.grpc.Status;

public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }

    @Override
    public Status.Code code() {
        return Status.Code.ABORTED;
    }
}
