#!/usr/bin/env bash
# Shared helpers for the scripts in this folder. Needs: bash, curl, jq.
# Works on Linux, macOS, and Windows through Git Bash or WSL.

GATEWAY="${GATEWAY:-http://localhost:8080}"
MUNICIPAL="${MUNICIPAL:-http://localhost:8083}"
COLLECTION_ACTUATOR="${COLLECTION_ACTUATOR:-http://localhost:8082}"
FRONTEND="${FRONTEND:-http://localhost:3000}"
REPORT_DIRECT="${REPORT_DIRECT:-http://localhost:8081}"

for tool in curl jq; do
  command -v "$tool" >/dev/null || { echo "missing tool: $tool"; exit 2; }
done

pass() { echo "  ok: $*"; }
fail() { echo "  FAIL: $*" >&2; exit 1; }

assert_eq() {  # actual expected label
  [[ "$1" == "$2" ]] && pass "$3" || fail "$3 (expected '$2', got '$1')"
}
assert_contains() {  # haystack needle label
  [[ "$1" == *"$2"* ]] && pass "$3" || { echo "$1" >&2; fail "$3 (missing '$2')"; }
}

wait_for_url() {  # url name [attempts]
  local attempts="${3:-60}"
  for _ in $(seq 1 "$attempts"); do
    curl -fsS -o /dev/null "$1" 2>/dev/null && { pass "$2 is up"; return; }
    sleep 3
  done
  fail "$2 did not become ready"
}

# poll until a command prints the wanted value. usage: wait_until "description" wanted timeout_s command...
wait_until() {
  local label="$1" wanted="$2" timeout="$3"; shift 3
  local end=$((SECONDS + timeout)) got=""
  while (( SECONDS < end )); do
    got="$("$@" 2>/dev/null || true)"
    [[ "$got" == "$wanted" ]] && { pass "$label"; return; }
    sleep 2
  done
  fail "$label (waited ${timeout}s, last value '$got')"
}

api() {  # method path [json-body]  -> prints body, exits non-zero on HTTP >= 400
  local method="$1" path="$2" body="${3:-}"
  if [[ -n "$body" ]]; then
    curl -fsS -X "$method" "$GATEWAY$path" -H 'Content-Type: application/json' -d "$body"
  else
    curl -fsS -X "$method" "$GATEWAY$path"
  fi
}

http_status() {  # method path [json-body]
  local method="$1" path="$2" body="${3:-}"
  curl -s -o /dev/null -w '%{http_code}' -X "$method" "$GATEWAY$path" -H 'Content-Type: application/json' ${body:+-d "$body"}
}

new_report() {  # prints the id of a fresh SUBMITTED report
  api POST /api/v1/reports '{"reporterName":"Script","description":"Illegal dump (script)","wasteCategory":"BULKY","latitude":36.8,"longitude":10.18,"address":"Rue de Marseille"}' | jq -r .id
}

validate_report() {  # id -> prints the HTTP status of the validation (200 planned, 202 postponed)
  http_status POST "/api/v1/reports/$1/validate"
}

report_field()    { api GET "/api/v1/reports/$1" | jq -r ".$2"; }   # id field
report_status()   { report_field "$1" status; }
transmission_status()  { report_field "$1" transmissionStatus; }
transmission_ref()     { report_field "$1" transmissionReference; }

breaker_state() {  # breaker name, port of the service that owns it (report-service direct port)
  curl -fsS "$REPORT_DIRECT/actuator/circuitbreakers" | jq -r ".circuitBreakers.$1.state"
}

soap() {  # xml-body-of-envelope -> response (does not fail on SOAP faults)
  curl -sS -X POST "$MUNICIPAL/ws" -H 'Content-Type: text/xml; charset=utf-8' \
    -d "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:m=\"http://ecotrack.example/municipal\"><soapenv:Header/><soapenv:Body>$1</soapenv:Body></soapenv:Envelope>"
}
soap_record_found() {  # recordId -> "found>true<" when the municipal system knows it
  soap "<m:GetRecordStatusRequest><m:recordId>$1</m:recordId></m:GetRecordStatusRequest>" | grep -o 'found>[a-z]*<' | head -1
}
soap_submit() {  # recordId reportId -> raw response
  soap "<m:SubmitCollectionRecordRequest><m:recordId>$1</m:recordId><m:reportId>$2</m:reportId><m:collectionId>$1</m:collectionId><m:completedAt>2026-01-01T10:00:00Z</m:completedAt></m:SubmitCollectionRecordRequest>"
}
