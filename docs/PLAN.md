# Plan: tasks per person (from the assignment) and grading map

Contracts first: **Person B publishes the `.proto` and the XSD on day 1**, because Person A generates the gRPC and
SOAP clients from them (`mvn install` in `contracts/` generates both).

## Person A: report-service, gateway, clients, resilience, citizen space
| # | Task | Where |
|---|---|---|
| A1 | Skeleton: Spring Boot 3.3, Java 17, Actuator, stdout logs, env config | `report-service/` |
| A2 | Entities Report  and CollectionPoint, JPA repositories, PostgreSQL, Flyway | `domain/`, `repo/`, `db/migration/` |
| A3 | REST `/api/v1`: `/reports`, `/collection-points`, correct verbs and codes (201, 204, 404, 409) | `web/` |
| A4 | Bean Validation (`jakarta.*`), `@RestControllerAdvice`, one error format | `common/GlobalExceptionHandler` |
| A5 | OpenAPI (springdoc), spec exported to the repo | `contracts/openapi/` |
| A6 | api-gateway: routes `/api/v1/**`, timeouts, CORS | `api-gateway/` |
| A7 | gRPC client from B's `.proto`, explicit deadline on every call | `collection/CollectionClient` |
| A8 | SOAP client: `WebServiceTemplate`, JAXB from B's XSD, connect/read timeouts | `soap/` |
| A9 | Resilience: circuit breaker on gRPC (fallback `PENDING_PLANNING`), retry with backoff on SOAP (fallback `PENDING_TRANSMISSION`) | `collection/ResilientGrpcCaller`, `soap/MunicipalClient`, `service/` |
| A10 | Tests: unit on business logic, REST integration (MockMvc), fallback tests | `report-service/src/test` |
| A11 | Bonus: JWT on the gateway | not done |
| FA1 | Frontend init, HTTP client, env variable for the gateway URL | `frontend/src/api` |
| FA2 | Report form (location, waste type, photo) with validation | `NewReportPage` |
| FA3 | Report list and detail with status | `MyReportsPage`, `ReportDetailPage` |
| FA4 | Map of collection points | `PointsMapPage` |
| FA5 | SSE endpoint in report-service | `events/SseHub`, `web/EventsController` |

## Person B: collection-service, municipal-service, infrastructure, agent space
| # | Task | Where |
|---|---|---|
| B1 | Versioned `.proto` (`v1`): `PlanCollection`, `GetCollection` (unary), `WatchStatus` (server streaming) | `contracts/src/main/proto` |
| B2 | gRPC server, PostgreSQL, `NOT_FOUND` / `INVALID_ARGUMENT`, simulated status evolution for the stream | `collection-service/` |
| B3 | XSD first: `SubmitCollectionRecord`, `GetRecordStatus`, `RecordRejectedFault` | `contracts/src/main/resources/municipal.xsd` |
| B4 | municipal-service: Spring-WS, WSDL generated from the XSD, 2 operations, SOAP Fault, persistence | `municipal-service/` |
| B5 | Multi-stage Dockerfile for each service | `*/Dockerfile` |
| B6 | `docker-compose.yml`: dedicated network, PostgreSQL volume, healthchecks, `depends_on: service_healthy`, `.env` and `.env.example` | repo root |
| B7 | Tests: gRPC and SOAP unit tests, gRPC integration (in-process), SOAP integration (`spring-ws-test`) | `*/src/test` |
| B8 | Bonus: GitHub Actions (build and tests) | `.github/workflows` |
| FB1 | Agent dashboard: reports to validate | `AgentDashboardPage` |
| FB2 | Validate a report, which triggers collection planning | `AgentDashboardPage` -> `POST /reports/{id}/validate` |
| FB3 | Live collection status (consumes the SSE) | `AgentReportPage`, `useReportEvents` |
| FB4 | Municipal record status (`PENDING_TRANSMISSION`, sent) | `AgentDashboardPage`, `AgentReportPage` |
| FB5 | Frontend multi-stage Dockerfile (build then nginx), added to compose | `frontend/Dockerfile` |

## Order of work
1. Day 1: B publishes `.proto` and XSD (done in this repo). Both: repo, CI, `mvn verify` green on the skeleton.
2. A: A1-A5, A6. B: B2, B4, B5. In parallel.
3. A: A7-A9 once B's services answer. B: B6, B7.
4. Frontend FA/FB after the REST API is stable; SSE (FA5) before FB3.
5. Last: failure demo rehearsal, report, defense (both explain every service).

## Grading map
| Item | Pts | Where it is shown |
|---|---|---|
| Design | 3 | `docs/TOPIC-SHEET.md`, DB per service |
| REST | 3 | A3-A5, `contracts/openapi` |
| gRPC | 2.5 | B1-B2, A7, `scripts/grpcurl-examples.sh` |
| SOAP | 2.5 | B3-B4, A8, `contracts/wsdl` |
| Docker | 3 | B5-B6, FB5 |
| Cloud-native / resilience | 3 | A9, `scripts/failure-demo.sh`, Actuator |
| Quality / tests | 1.5 | A10, B7, `TESTING.md`, CI |
| Docs | 1.5 | README, contracts, scripts, report |

## Known limits (state them in the report)
- No saga: a crash between the gRPC answer and the database update leaves a collection without its report being marked; the deterministic `request_id` makes the retry safe.
- The collection status evolution is a simulation (timer), not real field data.
- Stream events are not replayed after a disconnect: the watcher reconnects and receives the current status first.
- One PostgreSQL container hosts three databases; no authentication (JWT is a bonus); photos live on one volume.
- Nothing was compiled or run when this was written: expect first-build fixes.
