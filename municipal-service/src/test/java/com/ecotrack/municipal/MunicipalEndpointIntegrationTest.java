package com.ecotrack.municipal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.ws.test.server.ResponseMatcher;
import org.springframework.xml.transform.StringSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

/** Drives the real SOAP stack (XSD validation, endpoint mapping, fault resolver) against a real PostgreSQL. */
@SpringBootTest
@Testcontainers
class MunicipalEndpointIntegrationTest {

    private static final String NS = "http://ecotrack.example/municipal";
    private static final Map<String, String> NSMAP = Map.of("m", NS);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    ApplicationContext context;

    MockWebServiceClient client;

    @BeforeEach
    void setUp() {
        client = MockWebServiceClient.createClient(context);
    }

    @Test
    void validTransmissionThenDuplicate() {
        String id = UUID.randomUUID().toString();
        client.sendRequest(withPayload(new StringSource(submit(id, "2026-01-01T10:00:00Z"))))
                .andExpect(noFault())
                .andExpect(xpath("string(//m:duplicate)", NSMAP).evaluatesTo("false"))
                .andExpect(xpath("string(//m:accepted)", NSMAP).evaluatesTo("true"));
        client.sendRequest(withPayload(new StringSource(submit(id, "2026-01-01T10:00:00Z"))))
                .andExpect(noFault())
                .andExpect(xpath("string(//m:duplicate)", NSMAP).evaluatesTo("true"));
    }

    @Test
    void statusIsFoundAfterTransmission() {
        String id = UUID.randomUUID().toString();
        client.sendRequest(withPayload(new StringSource(submit(id, "2026-01-01T10:00:00Z"))))
                .andExpect(noFault());
        client.sendRequest(withPayload(new StringSource(
                        "<m:GetRecordStatusRequest xmlns:m=\"" + NS + "\"><m:recordId>" + id
                                + "</m:recordId></m:GetRecordStatusRequest>")))
                .andExpect(noFault())
                .andExpect(xpath("string(//m:found)", NSMAP).evaluatesTo("true"))
                .andExpect(xpath("//m:status", NSMAP).evaluatesTo("RECEIVED"));
    }

    @Test
    void statusOfUnknownRecord() {
        client.sendRequest(withPayload(new StringSource(
                        "<m:GetRecordStatusRequest xmlns:m=\"" + NS + "\"><m:recordId>"
                                + UUID.randomUUID() + "</m:recordId></m:GetRecordStatusRequest>")))
                .andExpect(noFault())
                .andExpect(xpath("string(//m:found)", NSMAP).evaluatesTo("false"));
    }

    @Test
    void invalidIdentifierIsRejectedBySchemaValidationWithAFault() {
        client.sendRequest(withPayload(new StringSource(submit("not-a-uuid", "2026-01-01T10:00:00Z"))))
                .andExpect(clientOrSenderFault());
    }

    @Test
    void missingMandatoryElementIsAFault() {
        client.sendRequest(withPayload(new StringSource(
                        "<m:SubmitCollectionRecordRequest xmlns:m=\"" + NS + "\"><m:recordId>"
                                + UUID.randomUUID() + "</m:recordId></m:SubmitCollectionRecordRequest>")))
                .andExpect(clientOrSenderFault());
    }

    @Test
    void futureDateBreaksABusinessRuleAndReturnsRecordRejectedFault() {
        client.sendRequest(withPayload(new StringSource(submit(UUID.randomUUID().toString(), "2099-01-01T10:00:00Z"))))
                .andExpect(faultWithCode("RecordRejectedFault"));
    }

    private static ResponseMatcher faultWithCode(String localPart) {
        return (request, response) -> {
            SoapMessage message = (SoapMessage) response;
            assertThat(message.hasFault()).as("response is a SOAP fault").isTrue();
            assertThat(message.getSoapBody().getFault().getFaultCode().getLocalPart()).isEqualTo(localPart);
        };
    }

    private static String submit(String recordId, String completedAt) {
        return "<m:SubmitCollectionRecordRequest xmlns:m=\"" + NS + "\">"
                + "<m:recordId>" + recordId + "</m:recordId>"
                + "<m:reportId>" + UUID.randomUUID() + "</m:reportId>"
                + "<m:collectionId>" + UUID.randomUUID() + "</m:collectionId>"
                + "<m:completedAt>" + completedAt + "</m:completedAt>"
                + "<m:notes>test</m:notes></m:SubmitCollectionRecordRequest>";
    }
}