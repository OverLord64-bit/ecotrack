#!/usr/bin/env bash
# Refreshes contracts/openapi/report-service.yaml from the running service (springdoc generates it from the code).
set -euo pipefail
cd "$(dirname "$0")/.."
curl -fsS http://localhost:8081/v3/api-docs.yaml -o contracts/openapi/report-service.yaml
echo "written: contracts/openapi/report-service.yaml"
