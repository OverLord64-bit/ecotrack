# EcoTrack

Citizens report illegal dumping; agents validate reports, which plans a collection; the collection is followed live;
the closed record is sent to a simulated municipal system. Four services plus a frontend, three protocols.

```
browser -> frontend:3000 -> api-gateway:8080 --REST--> report-service:8081 --gRPC--> collection-service:9090
                                                              \--SOAP--> municipal-service:8083
```

## Prerequisites
JDK 17, Maven 3.9+, Node 20+, Docker with Compose, curl, jq (grpcurl optional). Any IDE works (standard Maven
multi-module build plus a plain Node frontend). Build the `contracts` module once so IDEs see the generated
gRPC and SOAP classes: `mvn -q install -DskipTests`.

## Run
```bash
cp .env.example .env        # optional; defaults work for a local demo
docker compose up --build   # one command
```
| URL | What |
|---|---|
| http://localhost:3000 | Frontend (citizen and agent spaces) |
| http://localhost:8080/swagger-ui.html | REST API docs through the gateway |
| http://localhost:8080/api/v1/reports | REST |
| localhost:9090 | gRPC (reflection on) |
| http://localhost:8083/ws/municipal.wsdl | SOAP WSDL |
| http://localhost:8083/ | Municipal records admin page |
| http://localhost:8081/actuator/circuitbreakers | circuit breaker state |

Reset data: `docker compose down -v`.

## Try it
```bash
bash scripts/smoke-test.sh      # full flow across REST, gRPC, SOAP
bash scripts/failure-demo.sh    # stop each downstream service, watch fallbacks, restart, no duplicate
bash scripts/grpcurl-examples.sh
```
Manual requests (REST and SOAP): `requests.http` (any HTTP client or IDE). Contracts: `contracts/`.

Demo flow in the UI: create a report (Citizen > Report), open Agent > Dashboard, click "Validate & plan", then
open the report to watch PLANNED, IN_PROGRESS, COMPLETED arrive live and the municipal record become Sent.

## Layout
```
contracts/            collection.proto, municipal.xsd, wsdl/, openapi/
api-gateway/          report-service/        collection-service/        municipal-service/
frontend/             React + TypeScript + Vite, nginx image
db/init/              creates the three databases
scripts/              smoke test, failure demo, grpcurl, contract export
docs/                 TOPIC-SHEET, PLAN (tasks per person), REPORT-OUTLINE
```
Configuration comes from environment variables (see `.env.example`); no secret is committed.

## Dev mode
`docker compose up postgres`, run each Spring Boot main class from your IDE (or `mvn -pl <module> spring-boot:run`),
then `cd frontend && npm install && npm run dev` (http://localhost:5173, `/api` is proxied to the gateway).
Defaults point to `localhost`; Compose overrides them.

## Team workflow
`main` always runs; short branches and pull requests reviewed by the other person; contract changes
(`contracts/`) need both approvals; the Git history must show both members. Person A and B tasks: `docs/PLAN.md`.

## Project checklist
Tick a box when the task works and is merged to `main`. Rows ticked below were verified by a `docker compose up` run and the first part of `scripts/smoke-test.sh`
(services healthy, REST contract, gateway CORS). Rows still open need a run, a green CI, or a deliverable.

| Done | Person | Task |
|---|---|---|
| [ ] | both | Repo, branch protection, CODEOWNERS handles filled in, topic sheet submitted to the teacher |
| [x] | B | B1 `.proto` v1 published on day 1 (unary + server streaming) |
| [x] | B | B3 XSD written first, published on day 1 |
| [x] | A | A1-A2 report-service skeleton, entities, Flyway migrations |
| [x] | A | A3-A4 REST API, verbs and codes, validation, single error format |
| [x] | A | A5 OpenAPI exported (`scripts/export-openapi.sh`) |
| [x] | A | A6 gateway routes, timeouts, CORS |
| [ ] | B | B2 collection-service gRPC server, status codes, simulated status stream |
| [ ] | B | B4 municipal-service SOAP, WSDL from the XSD, Fault, idempotency |
| [x] | A | A7 gRPC client with deadline on every call |
| [x] | A | A8 SOAP client with timeouts (JAXB from the XSD) |
| [ ] | A | A9 circuit breaker + fallback `PENDING_PLANNING`; retry/backoff + fallback `PENDING_TRANSMISSION` |
| [ ] | A | A10 unit, REST integration and fallback tests |
| [ ] | B | B7 gRPC and SOAP unit and integration tests |
| [x] | B | B5 multi-stage Dockerfiles |
| [x] | B | B6 compose: network, volume, healthchecks, `service_healthy`, `.env.example` |
| [x] | A | FA1-FA4 citizen space: form with photo, list, detail, collection points map |
| [x] | A | FA5 SSE endpoint |
| [x] | B | FB1-FB2 agent dashboard, validation triggers planning |
| [x] | B | FB3-FB4 live collection status (SSE), municipal record status |
| [x] | B | FB5 frontend Dockerfile (build then nginx) in compose |
| [ ] | both | `scripts/smoke-test.sh` and `scripts/failure-demo.sh` pass on a fresh clone |
| [ ] | both | CI green on a pull request (B8) |
| [ ] | both | Report PDF (8-12 pages), defense rehearsed, each member can explain every service |
| [ ] | A | A11 bonus: JWT on the gateway |

## Limits
See `docs/PLAN.md`.
