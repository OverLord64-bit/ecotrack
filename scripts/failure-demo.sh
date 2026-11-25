#!/usr/bin/env bash
# Resilience demonstration, both directions:
#   A. gRPC: stop collection-service -> validations are postponed (PENDING_PLANNING), the circuit breaker
#      opens; restart it -> the retry job plans them (PLANNED).
#   B. SOAP: stop municipal-service -> the record stays PENDING_TRANSMISSION (retried with backoff); restart it ->
#      TRANSMITTED, and the municipal system holds ONE record.
# Needs the stack from `docker compose up` and the docker CLI.  Usage: bash scripts/failure-demo.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./lib.sh
COMPOSE="docker compose -f ../docker-compose.yml"
attempts() { report_field "$1" transmissionAttempts; }

wait_for_url "$GATEWAY/actuator/health" "api-gateway"

echo "== A. collection-service goes down (gRPC, circuit breaker + fallback)"
$COMPOSE stop collection-service >/dev/null
pass "collection-service stopped"
WAITING=()
for _ in 1 2 3 4; do
  r="$(new_report)"; WAITING+=("$r")
  assert_eq "$(validate_report "$r")" "202" "validation answers 202 (accepted, postponed) instead of failing"
  assert_eq "$(report_status "$r")" "PENDING_PLANNING" "fallback status for $r"
done
wait_until "gRPC circuit breaker is OPEN (calls now fail fast)" "OPEN" 30 breaker_state collection

echo "   restarting collection-service"
$COMPOSE start collection-service >/dev/null
wait_for_url "$COLLECTION_ACTUATOR/actuator/health" "collection-service"
for r in "${WAITING[@]}"; do
  wait_until "report $r planned by the retry job" "PLANNED" 150 report_status "$r"
done
wait_until "gRPC circuit breaker closed again" "CLOSED" 60 breaker_state collection

echo "== B. municipal-service goes down (SOAP, retry with backoff + fallback)"
$COMPOSE stop municipal-service >/dev/null
pass "municipal-service stopped"
R="${WAITING[0]}"
wait_until "collection finished (RESOLVED) although the municipal system is down" "RESOLVED" 120 report_status "$R"
wait_until "record stays PENDING_TRANSMISSION" "PENDING_TRANSMISSION" 60 transmission_status "$R"
wait_until "attempts are counted" "yes" 60 bash -c "[[ \$(curl -fsS $GATEWAY/api/v1/reports/$R | jq -r .transmissionAttempts) -ge 1 ]] && echo yes"
echo "   last error: $(report_field "$R" transmissionError)"

echo "   restarting municipal-service"
$COMPOSE start municipal-service >/dev/null
wait_for_url "$MUNICIPAL/actuator/health" "municipal-service"
wait_until "record delivered by the retry job (TRANSMITTED)" "TRANSMITTED" 120 transmission_status "$R"
echo "   municipal reference: $(transmission_ref "$R")"

echo "== No duplicate"
COLLECTION="$(report_field "$R" collectionId)"
assert_eq "$(soap_record_found "$COLLECTION")" "found>true<" "the municipal system has the record"
assert_contains "$(soap_submit "$COLLECTION" "$R")" "duplicate>true<" "a replay is recognised as a duplicate: still ONE record"

echo; echo "Failure recovery demonstrated."
