CREATE TABLE IF NOT EXISTS collections (
  id             UUID PRIMARY KEY,
  request_id     UUID         NOT NULL UNIQUE,      -- idempotency key chosen by the caller
  report_id UUID         NOT NULL,
  address        VARCHAR(200),
  status         VARCHAR(20)  NOT NULL,             -- PLANNED | IN_PROGRESS | COMPLETED
  priority       VARCHAR(20)  NOT NULL,
  scheduled_for TIMESTAMPTZ  NOT NULL,
  created_at        TIMESTAMPTZ  NOT NULL,
  updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_collections_report ON collections (report_id);
