# Topic sheet: EcoTrack

**Team:** _name A_ (report side), _name B_ (collection and infrastructure)  **Course:** SOA 2026 mini-project

## Problem
Illegal waste dumping is reported by citizens, but municipal agents lack a traceable way to turn reports into
planned collections and to record each completed collection in the municipal information system.
EcoTrack lets citizens report dumping, lets agents validate a report (which plans a collection), follows the
collection live, and sends the closed record to the (simulated) municipal system, even when it is temporarily down.

## Services
| Service | Responsibility | Exposes | Own storage |
|---|---|---|---|
| api-gateway (8080) | Single entry point `/api/v1/**`, timeouts, CORS | REST (Spring Cloud Gateway) | none |
| report-service (8081) | Reports , collection points , validation workflow, live events | **REST** `/api/v1` + OpenAPI, SSE | PostgreSQL `reports` (Flyway) + photo volume |
| collection-service (gRPC 9090) | Collections: planning, status simulation, status stream | **gRPC** (`PlanCollection`, `GetCollection`, `WatchStatus`) | PostgreSQL `collections` |
| municipal-service (8083) | Simulated municipal system: receives records, idempotent | **SOAP** (`SubmitCollectionRecord`, `GetRecordStatus`, `RecordRejectedFault`) | PostgreSQL `municipal` |
| frontend (3000) | Citizen space and agent space | static site (nginx) | none |

## Protocol choices
- **REST**: public, resource-oriented API consumed by the browser; easy to test, document (OpenAPI) and route.
- **gRPC**: typed internal contract with deadlines and a server stream, which suits live collection status.
- **SOAP**: models a legacy partner system with a strict contract written first (XSD), a WSDL and Faults.

## Inter-service calls (two protocols)
1. report-service -> collection-service over **gRPC**: `PlanCollection` (unary, deadline 2 s, circuit breaker; fallback `PENDING_PLANNING`) and `WatchStatus` (server streaming).
2. report-service -> municipal-service over **SOAP** (`WebServiceTemplate`, JAXB classes generated from the XSD, connect/read timeouts, retry with backoff; fallback `PENDING_TRANSMISSION`).

## Architecture
```
browser --> frontend (nginx) --/api--> api-gateway --REST--> report-service --gRPC--> collection-service
                                                               |   ^------ WatchStatus stream ------|
                                                               |--SOAP--> municipal-service
 SSE /api/v1/events: report-service --> browser
 DB reports (report-service) | DB collections (collection-service) | DB municipal (municipal-service)
```
One PostgreSQL container hosts the three databases (separate databases and roles).
