-- Multi-tenancy: every business table belongs to one client company (tenant).
--
-- The tenant of a connection is set by the application with set_config('app.tenant_id', ...) every time a
-- connection is handed out (TenantAwareDataSource). Row-level security then hides every other tenant's rows and
-- rejects writes for another tenant; with no tenant set nothing matches. Inserts take their tenant_id from the
-- session, so mappers do not have to pass it. The application connects as a plain role (not the table owner,
-- not a superuser, no BYPASSRLS); migrations run as the owner, which is not subject to the policies.
--
-- Rows that exist before this migration (the seed data of V1) are assigned to tenant 1.

CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;


-- tax_rates
ALTER TABLE tax_rates ADD COLUMN tenant_id BIGINT;
UPDATE tax_rates SET tenant_id = 1;
ALTER TABLE tax_rates ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE tax_rates ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_tax_rates_tenant ON tax_rates (tenant_id);
ALTER TABLE tax_rates ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON tax_rates
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- categories
ALTER TABLE categories ADD COLUMN tenant_id BIGINT;
UPDATE categories SET tenant_id = 1;
ALTER TABLE categories ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE categories ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_categories_tenant ON categories (tenant_id);
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON categories
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- products
ALTER TABLE products ADD COLUMN tenant_id BIGINT;
UPDATE products SET tenant_id = 1;
ALTER TABLE products ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE products ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_products_tenant ON products (tenant_id);
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON products
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- promotions
ALTER TABLE promotions ADD COLUMN tenant_id BIGINT;
UPDATE promotions SET tenant_id = 1;
ALTER TABLE promotions ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE promotions ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_promotions_tenant ON promotions (tenant_id);
ALTER TABLE promotions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON promotions
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- Codes and numbers are unique per tenant, not across the whole system.
ALTER TABLE tax_rates DROP CONSTRAINT tax_rates_code_key;
ALTER TABLE tax_rates ADD CONSTRAINT tax_rates_code_key UNIQUE (tenant_id, code);
ALTER TABLE categories DROP CONSTRAINT categories_name_key;
ALTER TABLE categories ADD CONSTRAINT categories_name_key UNIQUE (tenant_id, name);
ALTER TABLE products DROP CONSTRAINT products_sku_key;
ALTER TABLE products ADD CONSTRAINT products_sku_key UNIQUE (tenant_id, sku);
ALTER TABLE products DROP CONSTRAINT products_barcode_key;
ALTER TABLE products ADD CONSTRAINT products_barcode_key UNIQUE (tenant_id, barcode);
ALTER TABLE promotions DROP CONSTRAINT promotions_code_key;
ALTER TABLE promotions ADD CONSTRAINT promotions_code_key UNIQUE (tenant_id, code);
DROP INDEX uq_promotions_coupon;
CREATE UNIQUE INDEX uq_promotions_coupon ON promotions (tenant_id, upper(coupon_code)) WHERE coupon_code IS NOT NULL;
