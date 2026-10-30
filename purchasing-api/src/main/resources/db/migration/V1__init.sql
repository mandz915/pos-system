CREATE TABLE suppliers (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(30)  NOT NULL UNIQUE,
    name       VARCHAR(150) NOT NULL,
    contact    VARCHAR(150),
    phone      VARCHAR(40),
    email      VARCHAR(150),
    address    VARCHAR(255),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE purchase_orders (
    id          BIGSERIAL PRIMARY KEY,
    po_number   VARCHAR(30)  NOT NULL UNIQUE,
    supplier_id BIGINT       NOT NULL REFERENCES suppliers (id),
    store_id    BIGINT       NOT NULL,
    status      VARCHAR(12)  NOT NULL DEFAULT 'DRAFT'
                CHECK (status IN ('DRAFT', 'ORDERED', 'PARTIAL', 'RECEIVED', 'CANCELLED')),
    note        VARCHAR(255),
    total_cost  NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_by  BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ordered_at  TIMESTAMPTZ,
    closed_at   TIMESTAMPTZ
);

CREATE INDEX idx_po_store_status ON purchase_orders (store_id, status);

CREATE TABLE po_lines (
    id           BIGSERIAL PRIMARY KEY,
    po_id        BIGINT        NOT NULL REFERENCES purchase_orders (id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL,
    ordered_qty  NUMERIC(14, 3) NOT NULL CHECK (ordered_qty > 0),
    received_qty NUMERIC(14, 3) NOT NULL DEFAULT 0,
    unit_cost    NUMERIC(12, 2) NOT NULL CHECK (unit_cost >= 0),
    UNIQUE (po_id, product_id)
);

CREATE TABLE goods_receipts (
    id          BIGSERIAL PRIMARY KEY,
    po_id       BIGINT       NOT NULL REFERENCES purchase_orders (id),
    note        VARCHAR(255),
    received_by BIGINT,
    received_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE goods_receipt_lines (
    id         BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT        NOT NULL REFERENCES goods_receipts (id) ON DELETE CASCADE,
    po_line_id BIGINT        NOT NULL REFERENCES po_lines (id),
    product_id BIGINT        NOT NULL,
    quantity   NUMERIC(14, 3) NOT NULL CHECK (quantity > 0),
    unit_cost  NUMERIC(12, 2) NOT NULL
);

CREATE SEQUENCE po_number_seq START 1;

CREATE TABLE outbox (
    id           BIGSERIAL PRIMARY KEY,
    topic        VARCHAR(100) NOT NULL,
    msg_key      VARCHAR(100),
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox (id) WHERE published_at IS NULL;
