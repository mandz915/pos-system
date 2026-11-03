CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;

ALTER TABLE notifications ADD COLUMN tenant_id BIGINT;
UPDATE notifications SET tenant_id = 1;
ALTER TABLE notifications ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE notifications ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_notifications_tenant ON notifications (tenant_id);
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notifications
    USING (tenant_id = pos_current_tenant() OR pos_system_mode())
    WITH CHECK (tenant_id = pos_current_tenant() OR pos_system_mode());
