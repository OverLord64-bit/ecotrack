#!/bin/bash
# Runs once, on the first start of the Postgres container. Each service gets its OWN database and its OWN role,
# and cannot read the others. Passwords come from the environment (see .env.example), never from this file.
set -euo pipefail

create_db() {  # name, password
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE ROLE $1 LOGIN PASSWORD '$2';
CREATE DATABASE $1 OWNER $1;
REVOKE ALL ON DATABASE $1 FROM PUBLIC;
SQL
}

create_db reports     "${REPORTS_DB_PASSWORD:?}"
create_db collections "${COLLECTIONS_DB_PASSWORD:?}"
create_db municipal   "${MUNICIPAL_DB_PASSWORD:?}"
