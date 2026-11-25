package com.ecotrack.collection.logic;

import com.ecotrack.collection.domain.CollectionState;
import com.ecotrack.collection.domain.ConflictException;
import com.ecotrack.collection.domain.Priority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Business logic + JPA mapping + schema.sql against a real PostgreSQL. */
@SpringBootTest(properties = {"ecotrack.simulation.step-seconds=0", "ecotrack.simulation.tick-ms=3600000"})
@Testcontainers
class CollectionLogicIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    CollectionLogic logic;

    private PlanCommand command(UUID requestId, UUID reportId) {
        return new PlanCommand(requestId, reportId, "rue X", Instant.now().plusSeconds(3600), Priority.NORMAL);
    }

    @Test
    void planIsIdempotentAndPersisted() {
        UUID requestId = UUID.randomUUID();
        var first = logic.plan(command(requestId, UUID.randomUUID()));
        var second = logic.plan(command(requestId, UUID.randomUUID()));
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(logic.get(first.getId()).getStatus()).isEqualTo(CollectionState.PLANNED);
    }

    @Test
    void secondActiveCollectionForTheSameReportIsRefused() {
        UUID report = UUID.randomUUID();
        logic.plan(command(UUID.randomUUID(), report));
        assertThatThrownBy(() -> logic.plan(command(UUID.randomUUID(), report))).isInstanceOf(ConflictException.class);
    }

    @Test
    void simulationRunsACollectionToTheEnd() {
        var c = logic.plan(command(UUID.randomUUID(), UUID.randomUUID()));
        logic.advanceDue();
        assertThat(logic.get(c.getId()).getStatus()).isEqualTo(CollectionState.IN_PROGRESS);
        logic.advanceDue();
        assertThat(logic.get(c.getId()).getStatus()).isEqualTo(CollectionState.COMPLETED);
        assertThat(logic.advanceDue()).isEmpty();
    }
}
