CREATE TABLE roles (
    id   SMALLSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('SUPERVISOR'), ('CASHIER');

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    pin_hash      VARCHAR(100),
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(150),
    store_id      BIGINT,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_username ON users (lower(username));
CREATE INDEX idx_users_store ON users (store_id);

CREATE TABLE user_roles (
    user_id BIGINT   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE audit_log (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT,
    username   VARCHAR(60),
    action     VARCHAR(60) NOT NULL,
    entity     VARCHAR(60),
    entity_id  VARCHAR(60),
    details    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_created ON audit_log (created_at DESC);
CREATE INDEX idx_audit_user ON audit_log (user_id);
