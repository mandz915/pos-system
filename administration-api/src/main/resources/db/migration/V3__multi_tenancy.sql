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


-- stores
ALTER TABLE stores ADD COLUMN tenant_id BIGINT;
UPDATE stores SET tenant_id = 1;
ALTER TABLE stores ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE stores ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_stores_tenant ON stores (tenant_id);
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stores
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- terminals
ALTER TABLE terminals ADD COLUMN tenant_id BIGINT;
UPDATE terminals SET tenant_id = 1;
ALTER TABLE terminals ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE terminals ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_terminals_tenant ON terminals (tenant_id);
ALTER TABLE terminals ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON terminals
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- settings
ALTER TABLE settings ADD COLUMN tenant_id BIGINT;
UPDATE settings SET tenant_id = 1;
ALTER TABLE settings ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE settings ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_settings_tenant ON settings (tenant_id);
ALTER TABLE settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON settings
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- Codes and numbers are unique per tenant, not across the whole system.
ALTER TABLE stores DROP CONSTRAINT stores_code_key;
ALTER TABLE stores ADD CONSTRAINT stores_code_key UNIQUE (tenant_id, code);
ALTER TABLE terminals DROP CONSTRAINT terminals_code_key;
ALTER TABLE terminals ADD CONSTRAINT terminals_code_key UNIQUE (tenant_id, code);
ALTER TABLE settings DROP CONSTRAINT settings_pkey;
ALTER TABLE settings ADD PRIMARY KEY (tenant_id, store_id, setting_key);
