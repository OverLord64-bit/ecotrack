# Tests per development step

| Step | Tests that must exist | Where |
|---|---|---|
| Contracts | `.proto` compiles; XSD well-formed; WSDL generated; OpenAPI parses | `mvn verify`, CI `contracts-lint` |
| municipal-service | idempotent store, duplicate race, future date rule (unit); valid + duplicate, bad UUID Fault, missing element Fault, `RecordRejectedFault`, status query (integration, MockWebServiceClient + Testcontainers) | `RecordServiceTest`, `MunicipalEndpointIntegrationTest` |
| collection-service domain | status lifecycle; plan idempotent on `request_id`; validation; one active collection per report; simulation step | `CollectionStateTest`, `CollectionLogicTest` |
| collection-service gRPC | unary, status codes (INVALID_ARGUMENT, NOT_FOUND, ABORTED, INTERNAL), server streaming to completion (in-process server) | `CollectionGrpcServiceTest` |
| collection-service persistence | logic and schema against real PostgreSQL, simulation to COMPLETED | `CollectionLogicIntegrationTest` |
| report-service domain | status transition matrix; record lifecycle | `ReportStatusTest`, `ReportTest` |
| report-service services | reject rules, idempotent events, gRPC fallback (PENDING_PLANNING), SOAP fallbacks (PENDING_TRANSMISSION, REFUSED, TRANSMITTED), stable `request_id` | `ReportServiceTest`, `PlanningWorkflowTest`, `RecordTransmitterTest` |
| report-service SOAP client | Fault not retried; outage retried 3 times then fallback | `MunicipalClientResilienceTest` |
| report-service REST | 201/400/404/409/422/202, error shape, gRPC errors mapped to HTTP, photo, SSE opens, collection points CRUD, circuit breaker behavior | `ApiIntegrationTest` |
| gateway | routing | `GatewayRoutesTest` |
| frontend | typecheck and build | CI `frontend` |
| Docker | Dockerfile lint, compose config | CI |
| System | full flow, errors, CORS, SSE, photo; failure and recovery in both directions with no duplicate | `scripts/smoke-test.sh`, `scripts/failure-demo.sh` (CI `e2e`) |

Run: `mvn verify` (Docker needed for Testcontainers); `cd frontend && npm ci && npm run build`.
