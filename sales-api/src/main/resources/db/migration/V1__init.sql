CREATE TABLE shifts (
    id            BIGSERIAL PRIMARY KEY,
    store_id      BIGINT        NOT NULL,
    terminal_id   BIGINT        NOT NULL,
    cashier_id    BIGINT        NOT NULL,
    status        VARCHAR(10)   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    opening_cash  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    closing_cash  NUMERIC(12, 2),
    expected_cash NUMERIC(12, 2),
    opened_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    closed_at     TIMESTAMPTZ
);

-- A terminal can only have one open shift at a time.
CREATE UNIQUE INDEX uq_shift_open_terminal ON shifts (terminal_id) WHERE status = 'OPEN';

CREATE TABLE cash_movements (
    id         BIGSERIAL PRIMARY KEY,
    shift_id   BIGINT        NOT NULL REFERENCES shifts (id),
    type       VARCHAR(10)   NOT NULL CHECK (type IN ('CASH_IN', 'CASH_OUT')),
    amount     NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    reason     VARCHAR(255)  NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Gapless receipt numbering per store.
CREATE TABLE receipt_sequences (
    store_id BIGINT PRIMARY KEY,
    last_no  BIGINT NOT NULL
);

CREATE TABLE sales (
    id             BIGSERIAL PRIMARY KEY,
    client_ref     VARCHAR(64)   NOT NULL UNIQUE,
    receipt_no     VARCHAR(30) UNIQUE,
    store_id       BIGINT        NOT NULL,
    terminal_id    BIGINT        NOT NULL,
    shift_id       BIGINT        NOT NULL REFERENCES shifts (id),
    cashier_id     BIGINT        NOT NULL,
    customer_id    BIGINT,
    status         VARCHAR(12)   NOT NULL CHECK (status IN ('OPEN', 'HELD', 'COMPLETED', 'VOIDED', 'REFUNDED')),
    subtotal       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_total      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    paid_total     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    change_due     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    void_reason    VARCHAR(255),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_sales_store_created ON sales (store_id, created_at DESC);
CREATE INDEX idx_sales_shift ON sales (shift_id);

CREATE TABLE sale_lines (
    id                BIGSERIAL PRIMARY KEY,
    sale_id           BIGINT        NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    line_no           INT           NOT NULL,
    product_id        BIGINT        NOT NULL,
    sku               VARCHAR(50)   NOT NULL,
    name              VARCHAR(200)  NOT NULL,
    quantity          NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit_price        NUMERIC(12, 2) NOT NULL,
    discount_per_unit NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_total    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_rate          NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    tax_inclusive     BOOLEAN       NOT NULL DEFAULT TRUE,
    tax_amount        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    line_total        NUMERIC(12, 2) NOT NULL,
    returned_qty      NUMERIC(12, 3) NOT NULL DEFAULT 0
);

CREATE INDEX idx_sale_lines_sale ON sale_lines (sale_id);

CREATE TABLE sale_payments (
    id          BIGSERIAL PRIMARY KEY,
    sale_id     BIGINT        NOT NULL REFERENCES sales (id),
    method      VARCHAR(10)   NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL,
    tendered    NUMERIC(12, 2) NOT NULL,
    reference   VARCHAR(100),
    payment_id  BIGINT,
    gateway_ref VARCHAR(100),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_sale_payments_sale ON sale_payments (sale_id);

CREATE TABLE sale_returns (
    id           BIGSERIAL PRIMARY KEY,
    sale_id      BIGINT        NOT NULL REFERENCES sales (id),
    shift_id     BIGINT,
    refund_total NUMERIC(12, 2) NOT NULL,
    method       VARCHAR(10)   NOT NULL,
    reason       VARCHAR(255)  NOT NULL,
    restock      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by   BIGINT,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_sale_returns_sale ON sale_returns (sale_id);
CREATE INDEX idx_sale_returns_shift ON sale_returns (shift_id);

CREATE TABLE sale_return_lines (
    id           BIGSERIAL PRIMARY KEY,
    return_id    BIGINT        NOT NULL REFERENCES sale_returns (id) ON DELETE CASCADE,
    sale_line_id BIGINT        NOT NULL REFERENCES sale_lines (id),
    product_id   BIGINT        NOT NULL,
    quantity     NUMERIC(12, 3) NOT NULL,
    amount       NUMERIC(12, 2) NOT NULL
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
