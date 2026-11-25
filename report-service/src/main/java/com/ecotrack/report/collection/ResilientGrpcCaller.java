package com.ecotrack.report.collection;

import com.ecotrack.report.common.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Wraps every gRPC call with resilience. It is a separate bean on purpose: Resilience4j works through Spring
 * proxies, which only apply when a method is called from ANOTHER bean.
 *
 *  execute      retry with exponential backoff (outer) + circuit breaker (inner); for reads and idempotent calls
 *  executeOnce  circuit breaker only; for start/complete, which must not be replayed after a lost answer
 *
 * Outage-type failures (UNAVAILABLE, DEADLINE_EXCEEDED...) are retried and counted by the breaker.
 * Business errors (NOT_FOUND, FAILED_PRECONDITION...) become CollectionRejectedException, which is neither
 * retried nor counted. When everything fails, or the circuit is open, the fallback answers 503.
 */
@Component
public class ResilientGrpcCaller {

    private static final Logger log = LoggerFactory.getLogger(ResilientGrpcCaller.class);

    @Retry(name = "collection", fallbackMethod = "fallback")
    @CircuitBreaker(name = "collection")
    public <T> T execute(String operation, Supplier<T> call) {
        return invoke(call);
    }

    @CircuitBreaker(name = "collection", fallbackMethod = "fallback")
    public <T> T executeOnce(String operation, Supplier<T> call) {
        return invoke(call);
    }

    /** Public on purpose: Resilience4j finds the fallback by name and parameter types. */
    public <T> T fallback(String operation, Supplier<T> call, Exception cause) {
        if (cause instanceof CollectionRejectedException rejected) {
            throw rejected;
        }
        log.warn("collection-service call '{}' failed: {}", operation, cause.toString());
        throw new ServiceUnavailableException("collection-service is unavailable (" + operation + ")", cause);
    }

    private <T> T invoke(Supplier<T> call) {
        try {
            return call.get();
        } catch (StatusRuntimeException e) {
            switch (e.getStatus().getCode()) {
                case UNAVAILABLE, DEADLINE_EXCEEDED, CANCELLED, INTERNAL, UNKNOWN, RESOURCE_EXHAUSTED -> throw e;
                default -> throw new CollectionRejectedException(e.getStatus().getCode(), e.getStatus().getDescription());
            }
        }
    }
}
