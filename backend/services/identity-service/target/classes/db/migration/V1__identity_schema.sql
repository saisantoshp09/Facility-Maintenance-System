CREATE TABLE roles (
    name VARCHAR(30) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (name)
);

INSERT INTO roles (name) VALUES ('SYSTEM_ADMIN'), ('PROPERTY_MANAGER'), ('TENANT'), ('TECHNICIAN');

CREATE TABLE users (
    id BINARY(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    last_login_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_created_at ON users (created_at);

CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role_name VARCHAR(30) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_name),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_name) REFERENCES roles (name)
);

CREATE INDEX idx_user_roles_role ON user_roles (role_name);

CREATE TABLE audit_events (
    id BINARY(16) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_id BINARY(16) NULL,
    subject_id BINARY(16) NULL,
    outcome VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (id)
);

CREATE INDEX idx_audit_events_subject ON audit_events (subject_id);
CREATE INDEX idx_audit_events_created_at ON audit_events (created_at);
