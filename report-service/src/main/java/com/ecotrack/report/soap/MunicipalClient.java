package com.ecotrack.report.soap;

import com.ecotrack.contracts.municipal.SubmitCollectionRecordRequest;
import com.ecotrack.contracts.municipal.SubmitCollectionRecordResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

/**
 * The only place that calls the municipal SOAP service (WebServiceTemplate + JAXB classes generated from the XSD).
 *
 * Resilience: retry with exponential backoff. When attempts are exhausted the fallback converts the failure into
 * MunicipalUnavailableException, which the caller turns into the record status PENDING_TRANSMISSION (retried later by a
 * scheduled job). A SOAP Fault is an answer, not an outage: it is not retried and is rethrown as is.
 */
@Component
public class MunicipalClient {

    private static final Logger log = LoggerFactory.getLogger(MunicipalClient.class);

    private final WebServiceTemplate template;

    public MunicipalClient(WebServiceTemplate template) {
        this.template = template;
    }

    @Retry(name = "municipal", fallbackMethod = "fallback")
    public SubmitCollectionRecordResponse submit(SubmitCollectionRecordRequest request) {
        return (SubmitCollectionRecordResponse) template.marshalSendAndReceive(request);
    }

    /** Public on purpose: Resilience4j looks the fallback up by name and parameter types. */
    public SubmitCollectionRecordResponse fallback(SubmitCollectionRecordRequest request, Exception cause) {
        if (cause instanceof SoapFaultClientException fault) {
            throw fault;
        }
        log.warn("Municipal system unavailable: {}", cause.toString());
        throw new MunicipalUnavailableException("municipal system unavailable: " + cause.getClass().getSimpleName(), cause);
    }
}
