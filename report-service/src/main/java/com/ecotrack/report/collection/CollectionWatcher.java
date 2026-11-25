package com.ecotrack.report.collection;

import com.ecotrack.contracts.collection.v1.CollectionStatusEvent;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.service.CollectionStatusHandler;
import com.ecotrack.report.service.ReportService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Follows collections over the gRPC server stream WatchStatus. If the stream breaks, it reconnects after a delay;
 * after a restart, it resumes following every report that is PLANNED or IN_PROGRESS.
 */
@Component
public class CollectionWatcher {

    private static final Logger log = LoggerFactory.getLogger(CollectionWatcher.class);

    private final CollectionClient client;
    private final CollectionStatusHandler handler;
    private final ReportService reports;
    private final boolean enabled;
    private final long reconnectMs;
    private final Set<UUID> watching = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "collection-watch-reconnect");
        t.setDaemon(true);
        return t;
    });

    public CollectionWatcher(CollectionClient client, CollectionStatusHandler handler, ReportService reports,
                             @Value("${ecotrack.collection.watch.enabled:true}") boolean enabled,
                             @Value("${ecotrack.collection.watch.reconnect-ms:5000}") long reconnectMs) {
        this.client = client;
        this.handler = handler;
        this.reports = reports;
        this.enabled = enabled;
        this.reconnectMs = reconnectMs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resume() {
        if (!enabled) {
            return;
        }
        for (Report report : reports.followedByACollection()) {
            watch(report.getCollectionId());
        }
    }

    public void watch(UUID collectionId) {
        if (!enabled || !watching.add(collectionId)) {
            return;
        }
        connect(collectionId);
    }

    private void connect(UUID collectionId) {
        try {
            client.watch(collectionId.toString(), new StreamObserver<>() {
                @Override
                public void onNext(CollectionStatusEvent status) {
                    try {
                        handler.onStatus(collectionId, status.getStatus());
                    } catch (RuntimeException e) {
                        log.error("could not apply status of collection {}", collectionId, e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (Status.fromThrowable(t).getCode() == Status.Code.NOT_FOUND) {
                        log.warn("collection {} unknown to collection-service, no longer followed", collectionId);
                        watching.remove(collectionId);
                        return;
                    }
                    log.warn("status stream of collection {} broke ({}), reconnecting in {} ms", collectionId,
                            Status.fromThrowable(t).getCode(), reconnectMs);
                    scheduler.schedule(() -> connect(collectionId), reconnectMs, TimeUnit.MILLISECONDS);
                }

                @Override
                public void onCompleted() {
                    watching.remove(collectionId);   // COMPLETED: the server ends the stream
                }
            });
        } catch (RuntimeException e) {
            scheduler.schedule(() -> connect(collectionId), reconnectMs, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }
}
