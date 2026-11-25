package com.ecotrack.collection.grpc;

import com.ecotrack.contracts.collection.v1.CollectionStatus;
import com.ecotrack.contracts.collection.v1.CollectionStatusEvent;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory fan-out of status changes to the WatchStatus streams watching a given collection. */
@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final Map<UUID, Set<ServerCallStreamObserver<CollectionStatusEvent>>> subscribers = new ConcurrentHashMap<>();

    /** Registers the stream, then sends the current status. A finished collection is sent once and completed. */
    public void subscribe(UUID collectionId, StreamObserver<CollectionStatusEvent> observer, CollectionStatusEvent current) {
        ServerCallStreamObserver<CollectionStatusEvent> stream = (ServerCallStreamObserver<CollectionStatusEvent>) observer;
        if (current.getStatus() == CollectionStatus.COLLECTION_STATUS_COMPLETED) {
            synchronized (stream) {
                stream.onNext(current);
                stream.onCompleted();
            }
            return;
        }
        Set<ServerCallStreamObserver<CollectionStatusEvent>> set =
                subscribers.computeIfAbsent(collectionId, k -> ConcurrentHashMap.newKeySet());
        set.add(stream);
        stream.setOnCancelHandler(() -> set.remove(stream));
        synchronized (stream) {
            stream.onNext(current);
        }
    }

    public void publish(CollectionStatusEvent event) {
        UUID id = UUID.fromString(event.getCollectionId());
        Set<ServerCallStreamObserver<CollectionStatusEvent>> set = subscribers.get(id);
        if (set == null) {
            return;
        }
        boolean last = event.getStatus() == CollectionStatus.COLLECTION_STATUS_COMPLETED;
        for (ServerCallStreamObserver<CollectionStatusEvent> s : set) {
            try {
                synchronized (s) {
                    s.onNext(event);
                    if (last) {
                        s.onCompleted();
                    }
                }
            } catch (RuntimeException e) {
                log.debug("Dropping a closed subscriber: {}", e.toString());
                set.remove(s);
            }
        }
        if (last) {
            subscribers.remove(id);
        }
    }

    public int subscriberCount() {
        return subscribers.values().stream().mapToInt(Set::size).sum();
    }
}
