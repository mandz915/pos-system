CREATE TABLE stock_levels (
    id            BIGSERIAL PRIMARY KEY,
    store_id      BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    quantity      NUMERIC(14, 3) NOT NULL DEFAULT 0,
    reorder_level NUMERIC(14, 3) NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (store_id, product_id)
);

CREATE TABLE stock_movements (
    id            BIGSERIAL PRIMARY KEY,
    store_id      BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    type          VARCHAR(20)   NOT NULL,
    quantity      NUMERIC(14, 3) NOT NULL,
    balance_after NUMERIC(14, 3) NOT NULL,
    ref_type      VARCHAR(30),
    ref_id        VARCHAR(60),
    note          VARCHAR(255),
    created_by    BIGINT,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_movements_product ON stock_movements (store_id, product_id, id DESC);

CREATE TABLE stock_counts (
    id         BIGSERIAL PRIMARY KEY,
    store_id   BIGINT      NOT NULL,
    status     VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    posted_at  TIMESTAMPTZ
);

CREATE TABLE stock_count_lines (
    id          BIGSERIAL PRIMARY KEY,
    count_id    BIGINT        NOT NULL REFERENCES stock_counts (id) ON DELETE CASCADE,
    product_id  BIGINT        NOT NULL,
    system_qty  NUMERIC(14, 3),
    counted_qty NUMERIC(14, 3) NOT NULL,
    UNIQUE (count_id, product_id)
);

CREATE TABLE processed_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE outbox (
    id           BIGSERIAL PRIMARY KEY,
    topic        VARCHAR(100) NOT NULL,
    msg_key      VARCHAR(100),
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox (id) WHERE published_at IS NULL;
