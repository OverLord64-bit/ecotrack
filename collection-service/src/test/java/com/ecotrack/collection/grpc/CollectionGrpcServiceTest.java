package com.ecotrack.collection.grpc;

import com.ecotrack.collection.domain.CollectionJob;
import com.ecotrack.collection.domain.ConflictException;
import com.ecotrack.collection.domain.NotFoundException;
import com.ecotrack.collection.domain.Priority;
import com.ecotrack.collection.logic.CollectionLogic;
import com.ecotrack.contracts.collection.v1.CollectionStatus;
import com.ecotrack.contracts.collection.v1.CollectionServiceGrpc;
import com.ecotrack.contracts.collection.v1.GetCollectionRequest;
import com.ecotrack.contracts.collection.v1.PlanCollectionRequest;
import com.ecotrack.contracts.collection.v1.CollectionStatusEvent;
import com.ecotrack.contracts.collection.v1.WatchStatusRequest;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * gRPC integration test: a real gRPC server and client over the in-process transport, business logic mocked.
 * Checks unary calls, status codes and server streaming through the real gRPC stack.
 */
class CollectionGrpcServiceTest {

    private final CollectionLogic logic = mock(CollectionLogic.class);
    private final EventBus events = new EventBus();

    private Server server;
    private ManagedChannel channel;
    private CollectionServiceGrpc.CollectionServiceBlockingStub blocking;
    private CollectionServiceGrpc.CollectionServiceStub async;

    @BeforeEach
    void start() throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new CollectionGrpcService(logic, events)).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        blocking = CollectionServiceGrpc.newBlockingStub(channel);
        async = CollectionServiceGrpc.newStub(channel);
    }

    @AfterEach
    void stop() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    private static CollectionJob domain() {
        return new CollectionJob(UUID.randomUUID(), UUID.randomUUID(), "rue X", Priority.NORMAL,
                Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static PlanCollectionRequest planRequest() {
        return PlanCollectionRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString()).setReportId(UUID.randomUUID().toString())
                .setAddress("rue X").setScheduledFor(Timestamp.newBuilder().setSeconds(1_900_000_000L)).build();
    }

    private static Status.Code codeOf(StatusRuntimeException e) {
        return e.getStatus().getCode();
    }

    @Test
    void planCollectionReturnsThePlannedCollection() {
        CollectionJob c = domain();
        when(logic.plan(any())).thenReturn(c);
        var reply = blocking.planCollection(planRequest());
        assertThat(reply.getId()).isEqualTo(c.getId().toString());
        assertThat(reply.getStatus()).isEqualTo(CollectionStatus.COLLECTION_STATUS_PLANNED);
    }

    @Test
    void malformedIdentifierIsInvalidArgument() {
        var bad = PlanCollectionRequest.newBuilder().setRequestId("nope").build();
        assertThat(codeOf(assertThrows(StatusRuntimeException.class, () -> blocking.planCollection(bad))))
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(codeOf(assertThrows(StatusRuntimeException.class,
                () -> blocking.getCollection(GetCollectionRequest.newBuilder().setCollectionId("nope").build()))))
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void unknownCollectionIsNotFound() {
        when(logic.get(any())).thenThrow(new NotFoundException("missing"));
        var request = GetCollectionRequest.newBuilder().setCollectionId(UUID.randomUUID().toString()).build();
        assertThat(codeOf(assertThrows(StatusRuntimeException.class, () -> blocking.getCollection(request))))
                .isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void conflictIsAborted() {
        when(logic.plan(any())).thenThrow(new ConflictException("already planned"));
        assertThat(codeOf(assertThrows(StatusRuntimeException.class, () -> blocking.planCollection(planRequest()))))
                .isEqualTo(Status.Code.ABORTED);
    }

    @Test
    void unexpectedErrorIsInternalAndLeaksNothing() {
        when(logic.plan(any())).thenThrow(new IllegalStateException("secret SQL detail"));
        var e = assertThrows(StatusRuntimeException.class, () -> blocking.planCollection(planRequest()));
        assertThat(codeOf(e)).isEqualTo(Status.Code.INTERNAL);
        assertThat(e.getStatus().getDescription()).doesNotContain("secret");
    }

    @Test
    void watchStatusStreamsCurrentThenChangesThenCompletes() throws Exception {
        CollectionJob c = domain();
        when(logic.get(c.getId())).thenReturn(c);
        BlockingQueue<Object> received = new LinkedBlockingQueue<>();
        async.watchStatus(WatchStatusRequest.newBuilder().setCollectionId(c.getId().toString()).build(),
                new StreamObserver<>() {
                    public void onNext(CollectionStatusEvent v) { received.add(v); }
                    public void onError(Throwable t) { received.add(t); }
                    public void onCompleted() { received.add("done"); }
                });

        assertThat(((CollectionStatusEvent) received.poll(2, TimeUnit.SECONDS)).getStatus())
                .isEqualTo(CollectionStatus.COLLECTION_STATUS_PLANNED);

        c.advance(Instant.now());
        events.publish(GrpcMapper.event(c));
        assertThat(((CollectionStatusEvent) received.poll(2, TimeUnit.SECONDS)).getStatus())
                .isEqualTo(CollectionStatus.COLLECTION_STATUS_IN_PROGRESS);

        c.advance(Instant.now());
        events.publish(GrpcMapper.event(c));
        assertThat(((CollectionStatusEvent) received.poll(2, TimeUnit.SECONDS)).getStatus())
                .isEqualTo(CollectionStatus.COLLECTION_STATUS_COMPLETED);
        assertThat(received.poll(2, TimeUnit.SECONDS)).isEqualTo("done");
        assertThat(events.subscriberCount()).isZero();
    }

    @Test
    void watchStatusOfUnknownCollectionFailsWithNotFound() throws Exception {
        when(logic.get(any())).thenThrow(new NotFoundException("missing"));
        BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();
        async.watchStatus(WatchStatusRequest.newBuilder().setCollectionId(UUID.randomUUID().toString()).build(),
                new StreamObserver<>() {
                    public void onNext(CollectionStatusEvent v) { }
                    public void onError(Throwable t) { errors.add(t); }
                    public void onCompleted() { }
                });
        Throwable t = errors.poll(2, TimeUnit.SECONDS);
        assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }
}
