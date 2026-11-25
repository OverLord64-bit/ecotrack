#!/usr/bin/env bash
# gRPC calls straight to collection-service (port 9090), without going through REST. Needs grpcurl.
# Server reflection is enabled, so no .proto file is needed.  Usage: bash scripts/grpcurl-examples.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./lib.sh
command -v grpcurl >/dev/null || { echo "install grpcurl: https://github.com/fullstorydev/grpcurl"; exit 2; }
G="grpcurl -plaintext localhost:9090"
uuid() { cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen; }
FUTURE="$(date -u -d '+2 days' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+2d +%Y-%m-%dT%H:%M:%SZ)"

echo "--- services and methods (reflection)"
$G list
$G describe ecotrack.collection.v1.CollectionService

echo "--- unary: PlanCollection"
RESPONSE="$($G -d "{\"requestId\":\"$(uuid)\",\"reportId\":\"$(uuid)\",\"address\":\"rue X\",\"scheduledFor\":\"$FUTURE\",\"priority\":\"PRIORITY_HIGH\"}" ecotrack.collection.v1.CollectionService/PlanCollection)"
echo "$RESPONSE"
ID="$(echo "$RESPONSE" | jq -r .id)"

echo "--- unary: GetCollection"
$G -d "{\"collectionId\":\"$ID\"}" ecotrack.collection.v1.CollectionService/GetCollection

echo "--- error: unknown collection -> NOT_FOUND"
$G -d '{"collectionId":"00000000-0000-0000-0000-000000000000"}' ecotrack.collection.v1.CollectionService/GetCollection || true

echo "--- error: malformed id -> INVALID_ARGUMENT"
$G -d '{"collectionId":"nope"}' ecotrack.collection.v1.CollectionService/GetCollection || true

echo "--- error: schedule in the past -> INVALID_ARGUMENT"
$G -d "{\"requestId\":\"$(uuid)\",\"reportId\":\"$(uuid)\",\"scheduledFor\":\"2020-01-01T00:00:00Z\"}" ecotrack.collection.v1.CollectionService/PlanCollection || true

echo "--- deadline: a call with a 1 ms budget -> DEADLINE_EXCEEDED"
grpcurl -plaintext -max-time 0.001 -d "{\"collectionId\":\"$ID\"}" localhost:9090 ecotrack.collection.v1.CollectionService/GetCollection || true

echo "--- server streaming: WatchStatus (PLANNED, then IN_PROGRESS, then COMPLETED, then the stream ends)"
$G -d "{\"collectionId\":\"$ID\"}" ecotrack.collection.v1.CollectionService/WatchStatus
