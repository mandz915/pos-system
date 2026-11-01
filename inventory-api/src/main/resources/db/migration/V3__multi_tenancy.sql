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


-- stock_levels
ALTER TABLE stock_levels ADD COLUMN tenant_id BIGINT;
UPDATE stock_levels SET tenant_id = 1;
ALTER TABLE stock_levels ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_levels ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_levels_tenant ON stock_levels (tenant_id);
ALTER TABLE stock_levels ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_levels
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- stock_movements
ALTER TABLE stock_movements ADD COLUMN tenant_id BIGINT;
UPDATE stock_movements SET tenant_id = 1;
ALTER TABLE stock_movements ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_movements ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_movements_tenant ON stock_movements (tenant_id);
ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_movements
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- stock_counts
ALTER TABLE stock_counts ADD COLUMN tenant_id BIGINT;
UPDATE stock_counts SET tenant_id = 1;
ALTER TABLE stock_counts ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_counts ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_counts_tenant ON stock_counts (tenant_id);
ALTER TABLE stock_counts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_counts
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- stock_count_lines
ALTER TABLE stock_count_lines ADD COLUMN tenant_id BIGINT;
UPDATE stock_count_lines SET tenant_id = 1;
ALTER TABLE stock_count_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_count_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_count_lines_tenant ON stock_count_lines (tenant_id);
ALTER TABLE stock_count_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_count_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- The relay copies this into a Kafka header so consumers work for the right tenant.
-- No row-level security: the relay publishes every tenant's events.
ALTER TABLE outbox ADD COLUMN tenant_id BIGINT;
