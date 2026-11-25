#!/usr/bin/env bash
# End-to-end test of the running stack (docker compose up --build), through the API gateway.
# Covers the three protocols: REST (client -> gateway -> report-service), gRPC (report-service -> collection-service,
# unary + server streaming) and SOAP (report-service -> municipal-service).  Usage: bash scripts/smoke-test.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./lib.sh

echo "== 1. Everything is up"
wait_for_url "$GATEWAY/actuator/health" "api-gateway"
wait_for_url "$GATEWAY/api/v1/reports" "report-service through the gateway"
wait_for_url "$MUNICIPAL/actuator/health" "municipal-service"
wait_for_url "$COLLECTION_ACTUATOR/actuator/health" "collection-service"
wait_for_url "$FRONTEND/" "frontend"
assert_contains "$(curl -fsS "$FRONTEND/api/v1/collection-points")" "Centre Ville" "frontend nginx proxies /api to the gateway"

echo "== 2. REST contract (OpenAPI, validation, error shape, versioned path, CORS)"
assert_contains "$(curl -fsS "$GATEWAY/v3/api-docs")" '"/api/v1/reports"' "OpenAPI document is served and lists /api/v1/reports"
assert_eq "$(http_status POST /api/v1/reports '{"description":""}')" "422" "invalid body gives 422"
assert_eq "$(http_status GET /api/v1/reports/00000000-0000-0000-0000-000000000000)" "404" "unknown report gives 404"
assert_contains "$(curl -s "$GATEWAY/api/v1/reports/00000000-0000-0000-0000-000000000000")" '"code":"NOT_FOUND"' "errors use the standard shape"
# HTTP header names are case-insensitive (HTTP/1.1 sends them capitalised, HTTP/2 lowercase), so compare lowercased
PREFLIGHT="$(curl -s -D - -o /dev/null -X OPTIONS "$GATEWAY/api/v1/reports" -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST' | tr 'A-Z' 'a-z')"
assert_contains "$PREFLIGHT" "access-control-allow-origin: http://localhost:5173" "the gateway answers CORS preflight requests"

echo "== 3. SOAP contract (WSDL generated from the XSD, Faults)"
WSDL="$(curl -fsS "$MUNICIPAL/ws/municipal.wsdl")"
assert_contains "$WSDL" "SubmitCollectionRecord" "WSDL lists SubmitCollectionRecord"
assert_contains "$WSDL" "GetRecordStatus" "WSDL lists GetRecordStatus"
assert_contains "$(soap '<m:SubmitCollectionRecordRequest><m:recordId>not-a-uuid</m:recordId></m:SubmitCollectionRecordRequest>')" "Fault" "a message that breaks the XSD returns a SOAP Fault"
FUTURE="$(soap '<m:SubmitCollectionRecordRequest><m:recordId>11111111-1111-1111-1111-111111111111</m:recordId><m:reportId>11111111-1111-1111-1111-111111111111</m:reportId><m:collectionId>11111111-1111-1111-1111-111111111111</m:collectionId><m:completedAt>2099-01-01T10:00:00Z</m:completedAt></m:SubmitCollectionRecordRequest>')"
assert_contains "$FUTURE" "RecordRejectedFault" "a business rule violation returns RecordRejectedFault"

echo "== 4. Business flow: report -> validate (gRPC) -> stream -> resolved -> record (SOAP)"
REPORT="$(new_report)"
assert_eq "$(report_status "$REPORT")" "SUBMITTED" "new report is SUBMITTED"
assert_eq "$(validate_report "$REPORT")" "200" "validation plans the collection over gRPC"
assert_eq "$(report_status "$REPORT")" "PLANNED" "report became PLANNED"
COLLECTION="$(report_field "$REPORT" collectionId)"
[[ "$COLLECTION" != "null" && -n "$COLLECTION" ]] && pass "collection id: $COLLECTION" || fail "no collection id"
assert_eq "$(validate_report "$REPORT")" "409" "validating twice is refused (409)"

wait_until "report became IN_PROGRESS (gRPC server stream)" "IN_PROGRESS" 90 report_status "$REPORT"
wait_until "report became RESOLVED (gRPC server stream)" "RESOLVED" 90 report_status "$REPORT"
wait_until "record TRANSMITTED over SOAP" "TRANSMITTED" 60 transmission_status "$REPORT"
REF="$(transmission_ref "$REPORT")"
assert_contains "$REF" "MUN-" "municipal reference received: $REF"
assert_eq "$(api GET "/api/v1/reports/$REPORT/collection" | jq -r .status)" "COMPLETED" "GET /collection reads COMPLETED over gRPC"

echo "== 5. The municipal system stored it, exactly once"
assert_eq "$(soap_record_found "$COLLECTION")" "found>true<" "GetRecordStatus finds the record"
DUP="$(soap_submit "$COLLECTION" "$REPORT")"
assert_contains "$DUP" "duplicate>true<" "re-sending the same record is flagged duplicate"
assert_contains "$DUP" "$REF" "and returns the same reference"

echo "== 6. Rejection, photo, live events"
R2="$(new_report)"
assert_eq "$(http_status POST "/api/v1/reports/$R2/reject" '{"comment":""}')" "422" "rejecting without a comment is refused"
assert_eq "$(http_status POST "/api/v1/reports/$R2/reject" '{"comment":"duplicate"}')" "200" "rejection with a comment works"
assert_eq "$(http_status POST "/api/v1/reports/$R2/validate")" "409" "a rejected report cannot be validated"
PNG="$(mktemp)"; printf '\x89PNG\r\n\x1a\n' > "$PNG"
assert_eq "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GATEWAY/api/v1/reports/$R2/photo" -F "file=@$PNG;type=image/png")" "200" "photo upload works"
assert_eq "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GATEWAY/api/v1/reports/$R2/photo" -F "file=@$PNG;type=text/plain")" "422" "a non-image is refused"
rm -f "$PNG"
assert_contains "$(curl -s -N --max-time 3 -D - -o /dev/null "$GATEWAY/api/v1/events" || true)" "text/event-stream" "the SSE stream is reachable through the gateway"

echo "== 7. Health"
for url in "$GATEWAY/actuator/health" "$MUNICIPAL/actuator/health" "$COLLECTION_ACTUATOR/actuator/health" "$REPORT_DIRECT/actuator/health"; do
  assert_contains "$(curl -fsS "$url")" '"status":"UP"' "$url is UP"
done

echo; echo "All smoke tests passed."