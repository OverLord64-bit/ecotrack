package com.ecotrack.collection.domain;

import io.grpc.Status;

public class PreconditionFailedException extends DomainException {
    public PreconditionFailedException(String message) {
        super(message);
    }

    @Override
    public Status.Code code() {
        return Status.Code.FAILED_PRECONDITION;
    }
}
