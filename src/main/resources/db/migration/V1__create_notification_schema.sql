-- Current-state table: read/written on every API request and every Kafka consume.
CREATE TABLE notifications (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    type               VARCHAR(10)  NOT NULL CHECK (type IN ('EMAIL', 'SMS', 'PUSH')),
    message            VARCHAR(1000) NOT NULL,
    status             VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
                           CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING')),
    schedule_time      TIMESTAMP,
    retry_count        SMALLINT     NOT NULL DEFAULT 0 CHECK (retry_count BETWEEN 0 AND 3),
    last_attempted_at  TIMESTAMP,
    processed_at       TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON COLUMN notifications.schedule_time IS
    'Captured from the create request but not actively scheduled by any worker in this version — see README "Known limitations".';

-- Append-only audit trail: one row per send attempt (initial + manual retries).
CREATE TABLE notification_attempts (
    id               BIGSERIAL PRIMARY KEY,
    notification_id  BIGINT      NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    attempt_number   SMALLINT    NOT NULL,
    attempted_at     TIMESTAMP   NOT NULL DEFAULT now(),
    outcome          VARCHAR(10) NOT NULL CHECK (outcome IN ('SENT', 'FAILED')),
    failure_reason   VARCHAR(500),
    triggered_by     VARCHAR(20) NOT NULL CHECK (triggered_by IN ('INITIAL', 'MANUAL_RETRY'))
);

-- Single-column filters used directly by GET /api/notifications and GET /api/dashboard.
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_type ON notifications (type);

-- Default (unfiltered) list view, sorted by created date.
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);

-- Composite indexes: filter + sort satisfied by a single index walk.
CREATE INDEX idx_notifications_status_created_at ON notifications (status, created_at DESC);
CREATE INDEX idx_notifications_type_created_at ON notifications (type, created_at DESC);

-- Duplicate-detection lookup: equality on (user_id, type, message), range on created_at.
CREATE INDEX idx_notifications_dedup ON notifications (user_id, type, message, created_at DESC);

-- FK lookups are not auto-indexed in Postgres.
CREATE INDEX idx_notification_attempts_notification_id ON notification_attempts (notification_id);
