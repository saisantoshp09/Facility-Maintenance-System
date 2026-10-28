-- recipient_user_id and reference_id point at other services logically; there are no cross-service foreign keys.
CREATE TABLE processed_events (
    event_id BINARY(16) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id)
);

CREATE TABLE notifications (
    id BINARY(16) NOT NULL,
    event_id BINARY(16) NOT NULL,
    recipient_user_id BINARY(16) NOT NULL,
    type VARCHAR(64) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    reference_type VARCHAR(64) NULL,
    reference_id BINARY(16) NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL,
    next_attempt_at DATETIME(6) NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uq_notifications_event_recipient UNIQUE (event_id, recipient_user_id)
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id, created_at);
CREATE INDEX idx_notifications_delivery ON notifications (status, next_attempt_at);

CREATE TABLE notification_delivery_attempts (
    id BINARY(16) NOT NULL,
    notification_id BINARY(16) NOT NULL,
    attempt_no INT NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    error VARCHAR(500) NULL,
    attempted_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_notification_delivery_attempts PRIMARY KEY (id),
    CONSTRAINT fk_attempts_notification FOREIGN KEY (notification_id) REFERENCES notifications (id)
);

CREATE INDEX idx_attempts_notification ON notification_delivery_attempts (notification_id, attempt_no);
