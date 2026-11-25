package com.ecotrack.report.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes the raw SOAP envelopes to standard output (cloud-native logging) for the demo and the report. */
public class SoapLoggingInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger("soap");

    @Override
    public boolean handleRequest(MessageContext context) throws WebServiceClientException {
        log.info("SOAP request:\n{}", xml(context.getRequest()));
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext context) throws WebServiceClientException {
        log.info("SOAP response:\n{}", xml(context.getResponse()));
        return true;
    }

    @Override
    public boolean handleFault(MessageContext context) throws WebServiceClientException {
        log.warn("SOAP fault:\n{}", xml(context.getResponse()));
        return true;
    }

    @Override
    public void afterCompletion(MessageContext context, Exception ex) throws WebServiceClientException {
        // nothing to clean up
    }

    private static String xml(WebServiceMessage message) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            message.writeTo(out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unreadable>";
        }
    }
}
