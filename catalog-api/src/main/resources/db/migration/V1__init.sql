CREATE TABLE tax_rates (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    rate       NUMERIC(5, 2) NOT NULL CHECK (rate >= 0),
    inclusive  BOOLEAN      NOT NULL DEFAULT TRUE,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE categories (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories (id),
    active    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50)  NOT NULL UNIQUE,
    barcode     VARCHAR(64) UNIQUE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories (id),
    tax_rate_id BIGINT REFERENCES tax_rates (id),
    uom         VARCHAR(20)  NOT NULL DEFAULT 'pc',
    price       NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    cost        NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    image_url   VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_name ON products (lower(name));

CREATE TABLE promotions (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(10)  NOT NULL CHECK (type IN ('PERCENT', 'FIXED')),
    value       NUMERIC(12, 2) NOT NULL CHECK (value > 0),
    product_id  BIGINT REFERENCES products (id),
    category_id BIGINT REFERENCES categories (id),
    starts_at   TIMESTAMPTZ,
    ends_at     TIMESTAMPTZ,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO tax_rates (code, name, rate, inclusive) VALUES
    ('VAT12', 'VAT 12% (inclusive)', 12.00, TRUE),
    ('VAT0', 'Zero-rated / exempt', 0.00, TRUE);

INSERT INTO categories (name) VALUES ('General');
