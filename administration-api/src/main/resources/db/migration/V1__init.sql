CREATE TABLE stores (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    name       VARCHAR(150) NOT NULL,
    address    VARCHAR(255),
    phone      VARCHAR(40),
    tin        VARCHAR(30),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE terminals (
    id           BIGSERIAL PRIMARY KEY,
    store_id     BIGINT       NOT NULL REFERENCES stores (id),
    code         VARCHAR(30)  NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_terminals_store ON terminals (store_id);

-- store_id = 0 holds the global default; a row for a concrete store overrides it.
CREATE TABLE settings (
    store_id      BIGINT       NOT NULL DEFAULT 0,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT         NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (store_id, setting_key)
);

INSERT INTO settings (store_id, setting_key, setting_value) VALUES
    (0, 'currency', 'PHP'),
    (0, 'receipt.header', 'POS System'),
    (0, 'receipt.footer', 'Thank you for shopping with us!'),
    (0, 'tax.default_vat_rate', '12'),
    (0, 'loyalty.points_per_currency', '0.01');

INSERT INTO stores (code, name) VALUES ('MAIN', 'Main Store');
INSERT INTO terminals (store_id, code, name) VALUES (1, 'MAIN-T1', 'Terminal 1');
