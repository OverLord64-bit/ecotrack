package com.ecotrack.collection.logic;

import com.ecotrack.collection.grpc.EventBus;
import com.ecotrack.collection.grpc.GrpcMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Plays the role of the field teams: makes collections progress, then tells the connected streams. */
@Component
public class SimulationJob {

    private static final Logger log = LoggerFactory.getLogger(SimulationJob.class);

    private final CollectionLogic logic;
    private final EventBus events;

    public SimulationJob(CollectionLogic logic, EventBus events) {
        this.logic = logic;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${ecotrack.simulation.tick-ms:1000}")
    public void tick() {
        try {
            logic.advanceDue().forEach(c -> {
                log.info("collection {} is now {}", c.getId(), c.getStatus());
                events.publish(GrpcMapper.event(c));
            });
        } catch (RuntimeException e) {
            log.warn("simulation tick failed: {}", e.toString());
        }
    }
}
