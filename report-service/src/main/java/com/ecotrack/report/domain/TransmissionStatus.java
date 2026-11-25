package com.ecotrack.report.domain;

/** State of the record sent to the municipal system once a report is resolved. */
public enum TransmissionStatus {
    PENDING_TRANSMISSION,   // waiting to be sent (municipal system down, retried automatically)
    TRANSMITTED,        // accepted by the municipal system
    REFUSED           // the municipal system answered with a SOAP Fault: retrying would not help
}
