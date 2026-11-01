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


-- notifications
ALTER TABLE notifications ADD COLUMN tenant_id BIGINT;
UPDATE notifications SET tenant_id = 1;
ALTER TABLE notifications ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE notifications ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_notifications_tenant ON notifications (tenant_id);
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notifications
    USING (tenant_id = pos_current_tenant() OR pos_system_mode())
    WITH CHECK (tenant_id = pos_current_tenant() OR pos_system_mode());
