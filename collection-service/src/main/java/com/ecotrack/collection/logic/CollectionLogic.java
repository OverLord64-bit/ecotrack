package com.ecotrack.collection.logic;

import com.ecotrack.collection.domain.CollectionJob;
import com.ecotrack.collection.domain.CollectionState;
import com.ecotrack.collection.domain.ConflictException;
import com.ecotrack.collection.domain.InvalidRequestException;
import com.ecotrack.collection.domain.NotFoundException;
import com.ecotrack.collection.repo.CollectionJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CollectionLogic {

    private final CollectionJobRepository repository;
    private final Clock clock;
    private final Duration step;

    public CollectionLogic(CollectionJobRepository repository, Clock clock,
                           @Value("${ecotrack.simulation.step-seconds:10}") long stepSeconds) {
        this.repository = repository;
        this.clock = clock;
        this.step = Duration.ofSeconds(stepSeconds);
    }

    /** Idempotent on requestId: a retried call returns the collection created by the first one. */
    @Transactional
    public CollectionJob plan(PlanCommand command) {
        var already = repository.findByRequestId(command.requestId());
        if (already.isPresent()) {
            return already.get();
        }
        if (command.scheduledFor() == null || !command.scheduledFor().isAfter(clock.instant())) {
            throw new InvalidRequestException("scheduled_for must be in the future");
        }
        if (command.address() != null && command.address().length() > 200) {
            throw new InvalidRequestException("address is limited to 200 characters");
        }
        if (repository.existsByReportIdAndStatusNot(command.reportId(), CollectionState.COMPLETED)) {
            throw new ConflictException("report " + command.reportId() + " already has an active collection");
        }
        return repository.save(new CollectionJob(command.requestId(), command.reportId(), command.address(),
                command.priority(), command.scheduledFor(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public CollectionJob get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("collection " + id + " not found"));
    }

    /**
     * Status simulation: every collection that stayed in its state for at least `step` moves to the next state.
     * Returns the collections that changed so the caller can publish the events (after the commit).
     */
    @Transactional
    public List<CollectionJob> advanceDue() {
        Instant now = clock.instant();
        List<CollectionJob> changed = new ArrayList<>();
        for (CollectionJob c : repository.findByStatusNot(CollectionState.COMPLETED)) {
            if (!c.getUpdatedAt().plus(step).isAfter(now)) {
                c.advance(now);
                changed.add(c);
            }
        }
        return changed;
    }
}
