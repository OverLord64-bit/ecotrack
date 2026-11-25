package com.ecotrack.collection.logic;

import com.ecotrack.collection.domain.CollectionJob;
import com.ecotrack.collection.domain.CollectionState;
import com.ecotrack.collection.domain.ConflictException;
import com.ecotrack.collection.domain.InvalidRequestException;
import com.ecotrack.collection.domain.NotFoundException;
import com.ecotrack.collection.domain.Priority;
import com.ecotrack.collection.repo.CollectionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionLogicTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private CollectionJobRepository repository;
    private CollectionLogic logic;

    @BeforeEach
    void setUp() {
        repository = mock(CollectionJobRepository.class);
        logic = new CollectionLogic(repository, Clock.fixed(NOW, ZoneOffset.UTC), 10);
        when(repository.save(any(CollectionJob.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PlanCommand command(UUID requestId, Instant when) {
        return new PlanCommand(requestId, UUID.randomUUID(), "rue X", when, Priority.HIGH);
    }

    @Test
    void plansANewCollection() {
        CollectionJob c = logic.plan(command(UUID.randomUUID(), NOW.plusSeconds(3600)));
        assertThat(c.getStatus()).isEqualTo(CollectionState.PLANNED);
        verify(repository).save(any());
    }

    @Test
    void sameRequestIdReturnsTheExistingCollection() {
        UUID requestId = UUID.randomUUID();
        CollectionJob existing = new CollectionJob(requestId, UUID.randomUUID(), null, Priority.LOW, NOW.plusSeconds(60), NOW);
        when(repository.findByRequestId(requestId)).thenReturn(Optional.of(existing));

        assertThat(logic.plan(command(requestId, NOW.plusSeconds(3600)))).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsAScheduleInThePastOrMissing() {
        assertThatThrownBy(() -> logic.plan(command(UUID.randomUUID(), NOW.minusSeconds(1))))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> logic.plan(command(UUID.randomUUID(), null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsAReportThatAlreadyHasAnActiveCollection() {
        when(repository.existsByReportIdAndStatusNot(any(), any())).thenReturn(true);
        assertThatThrownBy(() -> logic.plan(command(UUID.randomUUID(), NOW.plusSeconds(60))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownCollectionIsNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> logic.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void simulationAdvancesOnlyCollectionsOlderThanTheStep() {
        CollectionJob old = new CollectionJob(UUID.randomUUID(), UUID.randomUUID(), null, Priority.NORMAL, NOW.plusSeconds(60), NOW.minusSeconds(11));
        CollectionJob recent = new CollectionJob(UUID.randomUUID(), UUID.randomUUID(), null, Priority.NORMAL, NOW.plusSeconds(60), NOW.minusSeconds(3));
        when(repository.findByStatusNot(CollectionState.COMPLETED)).thenReturn(List.of(old, recent));

        List<CollectionJob> changed = logic.advanceDue();

        assertThat(changed).containsExactly(old);
        assertThat(old.getStatus()).isEqualTo(CollectionState.IN_PROGRESS);
        assertThat(recent.getStatus()).isEqualTo(CollectionState.PLANNED);
    }
}
