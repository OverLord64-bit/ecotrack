#!/usr/bin/env bash
# Refreshes contracts/wsdl/municipal.wsdl from the running service (the WSDL is generated from the XSD by Spring-WS).
set -euo pipefail
cd "$(dirname "$0")/.."
curl -fsS http://localhost:8083/ws/municipal.wsdl -o contracts/wsdl/municipal.wsdl
echo "written: contracts/wsdl/municipal.wsdl"
