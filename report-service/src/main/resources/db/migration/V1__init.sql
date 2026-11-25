CREATE TABLE reports (
  id                UUID PRIMARY KEY,
  reporter_name     VARCHAR(120),
  description       TEXT NOT NULL,
  waste_category    VARCHAR(30) NOT NULL,
  latitude          DOUBLE PRECISION NOT NULL,
  longitude         DOUBLE PRECISION NOT NULL,
  address           VARCHAR(200),
  status            VARCHAR(30) NOT NULL,
  collection_id     UUID,                      -- reference to collection-service (no foreign key: other database)
  photo_filename    VARCHAR(100),
  resolved_at       TIMESTAMPTZ,
  transmission_status    VARCHAR(20),               -- NULL until resolved, then PENDING_TRANSMISSION | TRANSMITTED | REFUSED
  transmission_reference VARCHAR(40),               -- reference given by the municipal system
  transmission_attempts  INTEGER NOT NULL DEFAULT 0,
  transmission_error     TEXT,
  created_at        TIMESTAMPTZ NOT NULL,
  updated_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_transmission_status ON reports(transmission_status);

CREATE TABLE report_status_history (
  id         UUID PRIMARY KEY,
  report_id  UUID NOT NULL REFERENCES reports(id),
  old_status VARCHAR(30),
  new_status VARCHAR(30) NOT NULL,
  changed_by VARCHAR(80),
  changed_at TIMESTAMPTZ NOT NULL,
  comment    TEXT
);

CREATE TABLE collection_points (
  id          UUID PRIMARY KEY,
  name        VARCHAR(120) NOT NULL,
  description TEXT,
  latitude    DOUBLE PRECISION NOT NULL,
  longitude   DOUBLE PRECISION NOT NULL,
  address     VARCHAR(200),
  waste_types VARCHAR(200) NOT NULL,           -- comma-separated accepted categories
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL
);
