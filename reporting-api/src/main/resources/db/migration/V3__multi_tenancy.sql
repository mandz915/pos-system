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


-- sales_daily
ALTER TABLE sales_daily ADD COLUMN tenant_id BIGINT;
UPDATE sales_daily SET tenant_id = 1;
ALTER TABLE sales_daily ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sales_daily ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sales_daily_tenant ON sales_daily (tenant_id);
ALTER TABLE sales_daily ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sales_daily
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- product_daily
ALTER TABLE product_daily ADD COLUMN tenant_id BIGINT;
UPDATE product_daily SET tenant_id = 1;
ALTER TABLE product_daily ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE product_daily ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_product_daily_tenant ON product_daily (tenant_id);
ALTER TABLE product_daily ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON product_daily
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- cashier_daily
ALTER TABLE cashier_daily ADD COLUMN tenant_id BIGINT;
UPDATE cashier_daily SET tenant_id = 1;
ALTER TABLE cashier_daily ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE cashier_daily ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_cashier_daily_tenant ON cashier_daily (tenant_id);
ALTER TABLE cashier_daily ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON cashier_daily
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());
