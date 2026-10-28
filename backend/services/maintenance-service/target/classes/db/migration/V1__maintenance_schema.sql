-- property_id, unit_id and user ids reference other services logically; there are no cross-service foreign keys.
CREATE TABLE work_orders (
    id BINARY(16) NOT NULL,
    property_id BINARY(16) NOT NULL,
    unit_id BINARY(16) NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requester_id BINARY(16) NOT NULL,
    assignee_id BINARY(16) NULL,
    due_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_work_orders PRIMARY KEY (id)
);

CREATE INDEX idx_work_orders_property_status ON work_orders (property_id, status);
CREATE INDEX idx_work_orders_requester ON work_orders (requester_id);
CREATE INDEX idx_work_orders_assignee ON work_orders (assignee_id);
CREATE INDEX idx_work_orders_due ON work_orders (due_at);

CREATE TABLE work_order_comments (
    id BINARY(16) NOT NULL,
    work_order_id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_work_order_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_work_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id)
);

CREATE INDEX idx_comments_work_order ON work_order_comments (work_order_id, created_at);

CREATE TABLE work_order_status_history (
    id BINARY(16) NOT NULL,
    work_order_id BINARY(16) NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    actor_id BINARY(16) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_work_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_history_work_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id)
);

CREATE INDEX idx_history_work_order ON work_order_status_history (work_order_id, created_at);
