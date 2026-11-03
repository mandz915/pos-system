CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;

ALTER TABLE sales_daily ADD COLUMN tenant_id BIGINT;
UPDATE sales_daily SET tenant_id = 1;
ALTER TABLE sales_daily ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sales_daily ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sales_daily_tenant ON sales_daily (tenant_id);
ALTER TABLE sales_daily ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sales_daily
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE product_daily ADD COLUMN tenant_id BIGINT;
UPDATE product_daily SET tenant_id = 1;
ALTER TABLE product_daily ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE product_daily ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_product_daily_tenant ON product_daily (tenant_id);
ALTER TABLE product_daily ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON product_daily
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE cashier_daily ADD COLUMN tenant_id BIGINT;
UPDATE cashier_daily SET tenant_id = 1;
ALTER TABLE cashier_daily ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE cashier_daily ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_cashier_daily_tenant ON cashier_daily (tenant_id);
ALTER TABLE cashier_daily ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON cashier_daily
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());
