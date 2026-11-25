package com.ecotrack.collection.domain;

import io.grpc.Status;

/** Business error that knows which gRPC status code it maps to. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }

    public abstract Status.Code code();
}
