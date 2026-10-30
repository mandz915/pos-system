-- Read models built from Kafka events (sale completed / refunded). Safe to rebuild by replaying the topics.
CREATE TABLE sales_daily (
    store_id       BIGINT        NOT NULL,
    day            DATE          NOT NULL,
    sales_count    BIGINT        NOT NULL DEFAULT 0,
    gross          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_total      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    refund_count   BIGINT        NOT NULL DEFAULT 0,
    refund_total   NUMERIC(14, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (store_id, day)
);

CREATE TABLE product_daily (
    store_id   BIGINT        NOT NULL,
    day        DATE          NOT NULL,
    product_id BIGINT        NOT NULL,
    sku        VARCHAR(50),
    name       VARCHAR(200),
    quantity   NUMERIC(14, 3) NOT NULL DEFAULT 0,
    revenue    NUMERIC(14, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (store_id, day, product_id)
);

CREATE INDEX idx_product_daily_day ON product_daily (day);

CREATE TABLE cashier_daily (
    store_id    BIGINT        NOT NULL,
    day         DATE          NOT NULL,
    cashier_id  BIGINT        NOT NULL,
    sales_count BIGINT        NOT NULL DEFAULT 0,
    gross       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (store_id, day, cashier_id)
);

CREATE TABLE processed_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
