CREATE SEQUENCE IF NOT EXISTS record_ref_seq START WITH 1;

CREATE TABLE IF NOT EXISTS municipal_records (
  id                   BIGSERIAL PRIMARY KEY,
  reference            VARCHAR(30) NOT NULL UNIQUE,   -- e.g. MUN-2026-00042
  record_id           UUID        NOT NULL UNIQUE,   -- idempotency key: the EcoTrack collection id
  report_id       UUID        NOT NULL,
  collection_id          UUID        NOT NULL,
  completed_at          TIMESTAMPTZ NOT NULL,
  address              TEXT,
  notes                TEXT,
  received_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
