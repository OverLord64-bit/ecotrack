package com.ecotrack.collection.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectionStateTest {

    private static CollectionJob newCollection() {
        return new CollectionJob(UUID.randomUUID(), UUID.randomUUID(), "rue X", Priority.NORMAL,
                Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void followsPlanifieeEnCoursTerminee() {
        CollectionJob c = newCollection();
        assertThat(c.getStatus()).isEqualTo(CollectionState.PLANNED);
        c.advance(Instant.parse("2026-01-01T00:00:10Z"));
        assertThat(c.getStatus()).isEqualTo(CollectionState.IN_PROGRESS);
        c.advance(Instant.parse("2026-01-01T00:00:20Z"));
        assertThat(c.getStatus()).isEqualTo(CollectionState.COMPLETED);
        assertThat(c.getUpdatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:20Z"));
    }

    @Test
    void finishedCollectionNeverChanges() {
        CollectionJob c = newCollection();
        c.advance(Instant.now());
        c.advance(Instant.now());
        assertThatThrownBy(() -> c.advance(Instant.now())).isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void onlyTermineeIsFinal() {
        assertThat(CollectionState.PLANNED.isFinal()).isFalse();
        assertThat(CollectionState.IN_PROGRESS.isFinal()).isFalse();
        assertThat(CollectionState.COMPLETED.isFinal()).isTrue();
    }
}
