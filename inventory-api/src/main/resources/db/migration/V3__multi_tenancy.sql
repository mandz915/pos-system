CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;

ALTER TABLE stock_levels ADD COLUMN tenant_id BIGINT;
UPDATE stock_levels SET tenant_id = 1;
ALTER TABLE stock_levels ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_levels ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_levels_tenant ON stock_levels (tenant_id);
ALTER TABLE stock_levels ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_levels
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE stock_movements ADD COLUMN tenant_id BIGINT;
UPDATE stock_movements SET tenant_id = 1;
ALTER TABLE stock_movements ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_movements ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_movements_tenant ON stock_movements (tenant_id);
ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_movements
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE stock_counts ADD COLUMN tenant_id BIGINT;
UPDATE stock_counts SET tenant_id = 1;
ALTER TABLE stock_counts ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_counts ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_counts_tenant ON stock_counts (tenant_id);
ALTER TABLE stock_counts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_counts
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE stock_count_lines ADD COLUMN tenant_id BIGINT;
UPDATE stock_count_lines SET tenant_id = 1;
ALTER TABLE stock_count_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stock_count_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stock_count_lines_tenant ON stock_count_lines (tenant_id);
ALTER TABLE stock_count_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stock_count_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE outbox ADD COLUMN tenant_id BIGINT;
