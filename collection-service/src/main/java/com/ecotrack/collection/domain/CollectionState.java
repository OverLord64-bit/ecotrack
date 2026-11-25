package com.ecotrack.collection.domain;

/** Lifecycle of a collection: PLANNED -> IN_PROGRESS -> COMPLETED (no way back). */
public enum CollectionState {
    PLANNED, IN_PROGRESS, COMPLETED;

    public boolean isFinal() {
        return this == COMPLETED;
    }

    /** The only legal next state. */
    public CollectionState next() {
        return switch (this) {
            case PLANNED -> IN_PROGRESS;
            case IN_PROGRESS, COMPLETED -> COMPLETED;
        };
    }
}
