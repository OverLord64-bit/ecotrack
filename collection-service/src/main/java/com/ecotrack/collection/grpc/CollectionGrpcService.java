package com.ecotrack.collection.grpc;

import com.ecotrack.collection.domain.DomainException;
import com.ecotrack.collection.logic.CollectionLogic;
import com.ecotrack.contracts.collection.v1.CollectionInfo;
import com.ecotrack.contracts.collection.v1.CollectionServiceGrpc;
import com.ecotrack.contracts.collection.v1.GetCollectionRequest;
import com.ecotrack.contracts.collection.v1.PlanCollectionRequest;
import com.ecotrack.contracts.collection.v1.CollectionStatusEvent;
import com.ecotrack.contracts.collection.v1.WatchStatusRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Supplier;

@GrpcService
public class CollectionGrpcService extends CollectionServiceGrpc.CollectionServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(CollectionGrpcService.class);

    private final CollectionLogic logic;
    private final EventBus events;

    public CollectionGrpcService(CollectionLogic logic, EventBus events) {
        this.logic = logic;
        this.events = events;
    }

    @Override
    public void planCollection(PlanCollectionRequest request, StreamObserver<CollectionInfo> observer) {
        respond(observer, () -> GrpcMapper.toProto(logic.plan(GrpcMapper.toCommand(request))));
    }

    @Override
    public void getCollection(GetCollectionRequest request, StreamObserver<CollectionInfo> observer) {
        respond(observer, () -> GrpcMapper.toProto(logic.get(GrpcMapper.uuid(request.getCollectionId(), "collection_id"))));
    }

    @Override
    public void watchStatus(WatchStatusRequest request, StreamObserver<CollectionStatusEvent> observer) {
        try {
            UUID id = GrpcMapper.uuid(request.getCollectionId(), "collection_id");
            CollectionStatusEvent current = GrpcMapper.event(logic.get(id));
            events.subscribe(id, observer, current);
        } catch (DomainException e) {
            observer.onError(Status.fromCode(e.code()).withDescription(e.getMessage()).asRuntimeException());
        } catch (RuntimeException e) {
            log.error("Unexpected error in WatchStatus", e);
            observer.onError(Status.INTERNAL.withDescription("internal error").asRuntimeException());
        }
    }

    /** Business errors become precise gRPC status codes; anything else is INTERNAL and is logged. */
    private <T> void respond(StreamObserver<T> observer, Supplier<T> action) {
        try {
            T result = action.get();
            observer.onNext(result);
            observer.onCompleted();
        } catch (DomainException e) {
            observer.onError(Status.fromCode(e.code()).withDescription(e.getMessage()).asRuntimeException());
        } catch (RuntimeException e) {
            log.error("Unexpected error in gRPC handler", e);
            observer.onError(Status.INTERNAL.withDescription("internal error").asRuntimeException());
        }
    }
}
