package com.ecotrack.municipal.ws;

import com.ecotrack.contracts.municipal.GetRecordStatusRequest;
import com.ecotrack.contracts.municipal.GetRecordStatusResponse;
import com.ecotrack.contracts.municipal.SubmitCollectionRecordRequest;
import com.ecotrack.contracts.municipal.SubmitCollectionRecordResponse;
import com.ecotrack.municipal.records.RecordService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/** SOAP handler: routes by namespace + root element name. */
@Endpoint
public class MunicipalEndpoint {

    private final RecordService records;

    public MunicipalEndpoint(RecordService records) {
        this.records = records;
    }

    @PayloadRoot(namespace = WsConfig.NAMESPACE, localPart = "SubmitCollectionRecordRequest")
    @ResponsePayload
    public SubmitCollectionRecordResponse submit(@RequestPayload SubmitCollectionRecordRequest request) {
        return records.submit(request);
    }

    @PayloadRoot(namespace = WsConfig.NAMESPACE, localPart = "GetRecordStatusRequest")
    @ResponsePayload
    public GetRecordStatusResponse status(@RequestPayload GetRecordStatusRequest request) {
        return records.status(request.getRecordId());
    }
}
