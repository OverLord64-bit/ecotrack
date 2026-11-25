package com.ecotrack.municipal.records;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Business rejection of a well-formed record. Spring-WS turns it into a SOAP Fault whose fault code is the
 * qualified name {http://ecotrack.example/municipal}RecordRejectedFault (documented in the XSD).
 */
@SoapFault(faultCode = FaultCode.CUSTOM,
        customFaultCode = "{http://ecotrack.example/municipal}RecordRejectedFault")
public class InvalidRecordException extends RuntimeException {
    public InvalidRecordException(String message) {
        super(message);
    }
}
