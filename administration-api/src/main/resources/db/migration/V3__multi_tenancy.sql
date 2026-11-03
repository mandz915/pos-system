CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;

ALTER TABLE stores ADD COLUMN tenant_id BIGINT;
UPDATE stores SET tenant_id = 1;
ALTER TABLE stores ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stores ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stores_tenant ON stores (tenant_id);
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stores
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE terminals ADD COLUMN tenant_id BIGINT;
UPDATE terminals SET tenant_id = 1;
ALTER TABLE terminals ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE terminals ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_terminals_tenant ON terminals (tenant_id);
ALTER TABLE terminals ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON terminals
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE settings ADD COLUMN tenant_id BIGINT;
UPDATE settings SET tenant_id = 1;
ALTER TABLE settings ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE settings ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_settings_tenant ON settings (tenant_id);
ALTER TABLE settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON settings
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE stores DROP CONSTRAINT stores_code_key;
ALTER TABLE stores ADD CONSTRAINT stores_code_key UNIQUE (tenant_id, code);
ALTER TABLE terminals DROP CONSTRAINT terminals_code_key;
ALTER TABLE terminals ADD CONSTRAINT terminals_code_key UNIQUE (tenant_id, code);
ALTER TABLE settings DROP CONSTRAINT settings_pkey;
ALTER TABLE settings ADD PRIMARY KEY (tenant_id, store_id, setting_key);
