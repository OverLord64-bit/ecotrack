package com.ecotrack.collection.domain;

import io.grpc.Status;

public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public Status.Code code() {
        return Status.Code.NOT_FOUND;
    }
}
