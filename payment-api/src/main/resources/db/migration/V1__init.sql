CREATE TABLE payments (
    id          BIGSERIAL PRIMARY KEY,
    kind        VARCHAR(10)   NOT NULL CHECK (kind IN ('PAYMENT', 'REFUND')),
    sale_id     BIGINT        NOT NULL,
    store_id    BIGINT        NOT NULL,
    method      VARCHAR(10)   NOT NULL CHECK (method IN ('CASH', 'CARD', 'EWALLET')),
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    tendered    NUMERIC(12, 2),
    status      VARCHAR(10)   NOT NULL CHECK (status IN ('APPROVED', 'DECLINED')),
    reference   VARCHAR(100),
    gateway_ref VARCHAR(100),
    reason      VARCHAR(255),
    created_by  BIGINT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_sale ON payments (sale_id);
CREATE INDEX idx_payments_store_time ON payments (store_id, created_at);
