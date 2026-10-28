CREATE TABLE outbox_events (
    id BINARY(16) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INT NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    last_error VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status_next_attempt ON outbox_events (status, next_attempt_at);
