package com.ecotrack.report.soap;

import com.ecotrack.contracts.municipal.SubmitCollectionRecordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.ws.test.client.RequestMatchers.anything;
import static org.springframework.ws.test.client.ResponseCreators.withClientOrSenderFault;
import static org.springframework.ws.test.client.ResponseCreators.withException;

/** SOAP client against a simulated municipal system (MockWebServiceServer): fault, outage with retries, fallback. */
@Testcontainers
@SpringBootTest(properties = {
        "ecotrack.collection.watch.enabled=false",
        "ecotrack.transmission.retry-interval-ms=3600000",
        "ecotrack.planning.retry-interval-ms=3600000",
        "resilience4j.retry.instances.municipal.wait-duration=10ms"})
class MunicipalClientResilienceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MunicipalClient client;
    @Autowired
    private WebServiceTemplate template;

    private MockWebServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockWebServiceServer.createServer(template);
    }

    @Test
    void soapFaultIsRethrownAsIsAndNotRetried() {
        server.expect(anything()).andRespond(withClientOrSenderFault("record refused", Locale.ENGLISH));

        assertThrows(SoapFaultClientException.class, () -> client.submit(request()));

        server.verify();   // exactly one call: a Fault is an answer, not an outage
    }

    @Test
    void outageIsRetriedThreeTimesThenFallsBack() {
        for (int attempt = 0; attempt < 3; attempt++) {   // retry: max-attempts = 3
            server.expect(anything()).andRespond(withException(new IOException("connection refused")));
        }

        assertThrows(MunicipalUnavailableException.class, () -> client.submit(request()));

        server.verify();
    }

    private static SubmitCollectionRecordRequest request() {
        try {
            var request = new SubmitCollectionRecordRequest();
            request.setRecordId(UUID.randomUUID().toString());
            request.setCollectionId(UUID.randomUUID().toString());
            request.setReportId(UUID.randomUUID().toString());
            request.setCompletedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
            return request;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
