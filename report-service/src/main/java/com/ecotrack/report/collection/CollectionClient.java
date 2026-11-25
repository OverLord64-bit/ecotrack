package com.ecotrack.report.collection;

import com.ecotrack.contracts.collection.v1.CollectionInfo;
import com.ecotrack.contracts.collection.v1.CollectionServiceGrpc.CollectionServiceBlockingStub;
import com.ecotrack.contracts.collection.v1.CollectionServiceGrpc.CollectionServiceStub;
import com.ecotrack.contracts.collection.v1.GetCollectionRequest;
import com.ecotrack.contracts.collection.v1.PlanCollectionRequest;
import com.ecotrack.contracts.collection.v1.CollectionStatusEvent;
import com.ecotrack.contracts.collection.v1.WatchStatusRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * gRPC client of collection-service. Unary calls carry an explicit deadline and go through ResilientGrpcCaller
 * (circuit breaker + retry). The status stream has no deadline: it lives as long as the collection does.
 */
@Component
public class CollectionClient {

    @GrpcClient("collection")
    private CollectionServiceBlockingStub blockingStub;

    @GrpcClient("collection")
    private CollectionServiceStub asyncStub;

    private final ResilientGrpcCaller caller;
    private final long deadlineMs;

    public CollectionClient(ResilientGrpcCaller caller, @Value("${ecotrack.collection.deadline-ms:2000}") long deadlineMs) {
        this.caller = caller;
        this.deadlineMs = deadlineMs;
    }

    /** A deadline is absolute, so a fresh one is created for every attempt. */
    private CollectionServiceBlockingStub blocking() {
        return blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    public CollectionInfo plan(PlanCollectionRequest request) {
        return caller.execute("PlanCollection", () -> blocking().planCollection(request));
    }

    public CollectionInfo get(String id) {
        return caller.execute("GetCollection",
                () -> blocking().getCollection(GetCollectionRequest.newBuilder().setCollectionId(id).build()));
    }

    public void watch(String id, StreamObserver<CollectionStatusEvent> observer) {
        asyncStub.watchStatus(WatchStatusRequest.newBuilder().setCollectionId(id).build(), observer);
    }
}
