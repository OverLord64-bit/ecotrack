package com.ecotrack.report.web;

import com.ecotrack.contracts.collection.v1.CollectionStatus;
import com.ecotrack.contracts.collection.v1.CollectionInfo;
import com.ecotrack.report.collection.CollectionClient;
import com.ecotrack.report.collection.CollectionRejectedException;
import com.ecotrack.report.collection.ResilientGrpcCaller;
import com.ecotrack.report.common.ServiceUnavailableException;
import com.google.protobuf.Timestamp;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST integration test: real Spring MVC stack, validation, error handling, Flyway migrations and PostgreSQL
 * (Testcontainers). The gRPC client is mocked, except in the resilience tests, which use the real ResilientGrpcCaller.
 */
@Testcontainers
@SpringBootTest(properties = {
        "ecotrack.collection.watch.enabled=false",
        "ecotrack.transmission.retry-interval-ms=3600000",
        "ecotrack.planning.retry-interval-ms=3600000",
        "resilience4j.retry.instances.collection.wait-duration=10ms"})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ResilientGrpcCaller caller;
    @Autowired
    private CircuitBreakerRegistry circuitBreakers;
    @MockBean
    private CollectionClient collectionClient;

    @BeforeEach
    void resetBreaker() {
        circuitBreakers.circuitBreaker("collection").reset();
    }

    // ---------- reports: REST contract ----------

    @Test
    void createReturns201WithLocationAndStartsSubmitted() throws Exception {
        mvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(validReport()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void invalidReportIsRejectedWith422AndFieldDetails() throws Exception {
        mvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\",\"wasteCategory\":\"HOUSEHOLD\",\"latitude\":123,\"longitude\":10}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BODY"));
    }

    @Test
    void unknownReportIs404WithStandardErrorShape() throws Exception {
        mvc.perform(get("/api/v1/reports/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void listIsPaginatedAndFilterable() throws Exception {
        mvc.perform(get("/api/v1/reports?status=SUBMITTED&page=0&size=1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))     // page size is capped
                .andExpect(jsonPath("$.items[0].status").value("SUBMITTED"));
        mvc.perform(get("/api/v1/reports?status=NOPE")).andExpect(status().isBadRequest());
    }

    @Test
    void rejectNeedsACommentThenWorksAndCannotBeRepeated() throws Exception {
        String id = createReport();
        mvc.perform(post("/api/v1/reports/" + id + "/reject").contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/reports/" + id + "/reject").contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"duplicate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(post("/api/v1/reports/" + id + "/reject").contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        mvc.perform(get("/api/v1/reports/" + id + "/history"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- validation = planning over gRPC ----------

    @Test
    void validatePlansTheCollectionAndMarksPlanifie() throws Exception {
        String id = createReport();
        UUID collection = UUID.randomUUID();
        when(collectionClient.plan(any())).thenReturn(collectionInfo(collection, id));

        mvc.perform(post("/api/v1/reports/" + id + "/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.collectionId").value(collection.toString()));

        mvc.perform(post("/api/v1/reports/" + id + "/validate"))      // already planned
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_PLANNABLE"));
    }

    @Test
    void collectionServiceDownGives202AndEnAttentePlanification() throws Exception {
        String id = createReport();
        when(collectionClient.plan(any())).thenThrow(new ServiceUnavailableException("down", null));

        mvc.perform(post("/api/v1/reports/" + id + "/validate"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_PLANNING"));
    }

    @Test
    void businessRefusalFromGrpcIsMappedToHttp() throws Exception {
        String id = createReport();
        when(collectionClient.plan(any())).thenThrow(new CollectionRejectedException(Status.Code.ABORTED, "already active"));

        mvc.perform(post("/api/v1/reports/" + id + "/validate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COLLECTION_ABORTED"));
    }

    @Test
    void pastScheduleIsRejectedByValidationBeforeAnyGrpcCall() throws Exception {
        String id = createReport();
        mvc.perform(post("/api/v1/reports/" + id + "/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- photo + SSE ----------

    @Test
    void photoMustBeAnImage() throws Exception {
        String id = createReport();
        mvc.perform(multipart("/api/v1/reports/" + id + "/photo").file(new MockMultipartFile("file", "x.txt", "text/plain", "hi".getBytes())))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(multipart("/api/v1/reports/" + id + "/photo").file(new MockMultipartFile("file", "x.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("/api/v1/reports/" + id + "/photo"));
        mvc.perform(get("/api/v1/reports/" + id + "/photo")).andExpect(status().isOk()).andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void eventStreamIsOpened() throws Exception {
        mvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted());
    }

    // ---------- collection points ----------

    @Test
    void collectionPointCrud() throws Exception {
        String body = "{\"name\":\"Hub\",\"latitude\":36.8,\"longitude\":10.1,\"wasteTypes\":[\"BULKY\"]}";
        String created = mvc.perform(post("/api/v1/collection-points").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        mvc.perform(get("/api/v1/collection-points/" + id)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/collection-points/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/collection-points/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/collection-points").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- resilience of the gRPC call (real ResilientGrpcCaller) ----------

    @Test
    void outageIsRetriedThenFallsBackToServiceUnavailable() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(ServiceUnavailableException.class, () -> caller.execute("test", () -> {
            calls.incrementAndGet();
            throw new StatusRuntimeException(Status.UNAVAILABLE);
        }));
        assertEquals(3, calls.get());   // retry: max-attempts = 3
    }

    @Test
    void breakerOpensAfterRepeatedOutagesAndThenFailsFast() {
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            assertThrows(ServiceUnavailableException.class, () -> caller.execute("test", () -> {
                calls.incrementAndGet();
                throw new StatusRuntimeException(Status.UNAVAILABLE);
            }));
        }
        int before = calls.get();
        assertThrows(ServiceUnavailableException.class, () -> caller.execute("test", () -> {
            calls.incrementAndGet();
            return "never called";
        }));
        assertEquals(before, calls.get(), "an open circuit must not call the remote service");
    }

    @Test
    void businessErrorIsNotRetriedAndDoesNotTripTheBreaker() {
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            assertThrows(CollectionRejectedException.class, () -> caller.execute("test", () -> {
                calls.incrementAndGet();
                throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("nope"));
            }));
        }
        assertEquals(10, calls.get());   // one call each, no retries
        assertEquals("ok", caller.execute("test", () -> "ok"));   // breaker still closed
    }

    // ---------- helpers ----------

    private static CollectionInfo collectionInfo(UUID id, String reportId) {
        return CollectionInfo.newBuilder().setId(id.toString()).setReportId(reportId)
                .setStatus(CollectionStatus.COLLECTION_STATUS_PLANNED)
                .setScheduledFor(Timestamp.newBuilder().setSeconds(1_900_000_000L)).build();
    }

    private static String validReport() {
        return "{\"reporterName\":\"Amira\",\"description\":\"Mattresses on the pavement\",\"wasteCategory\":\"BULKY\","
                + "\"latitude\":36.8,\"longitude\":10.18,\"address\":\"Rue X\"}";
    }

    private String createReport() throws Exception {
        String body = mvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(validReport()))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
