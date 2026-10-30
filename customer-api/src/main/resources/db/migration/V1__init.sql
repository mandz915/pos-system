CREATE TABLE customers (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    name           VARCHAR(150) NOT NULL,
    phone          VARCHAR(40),
    email          VARCHAR(150),
    address        VARCHAR(255),
    points_balance BIGINT       NOT NULL DEFAULT 0 CHECK (points_balance >= 0),
    credit_limit   NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (credit_limit >= 0),
    credit_balance NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (credit_balance >= 0),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_customers_name ON customers (lower(name));
CREATE INDEX idx_customers_phone ON customers (phone);

CREATE TABLE loyalty_transactions (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers (id),
    type        VARCHAR(10) NOT NULL CHECK (type IN ('EARN', 'REDEEM', 'REVERSE', 'ADJUST')),
    points      BIGINT      NOT NULL,
    balance     BIGINT      NOT NULL,
    ref_type    VARCHAR(20),
    ref_id      VARCHAR(60),
    note        VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_loyalty_customer ON loyalty_transactions (customer_id, id DESC);

CREATE TABLE credit_transactions (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT        NOT NULL REFERENCES customers (id),
    type        VARCHAR(10)   NOT NULL CHECK (type IN ('CHARGE', 'PAYMENT')),
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    balance     NUMERIC(12, 2) NOT NULL,
    ref_type    VARCHAR(20),
    ref_id      VARCHAR(60),
    note        VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_credit_customer ON credit_transactions (customer_id, id DESC);

CREATE TABLE processed_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
