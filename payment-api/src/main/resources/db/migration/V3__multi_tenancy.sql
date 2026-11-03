CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;

ALTER TABLE payments ADD COLUMN tenant_id BIGINT;
UPDATE payments SET tenant_id = 1;
ALTER TABLE payments ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE payments ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_payments_tenant ON payments (tenant_id);
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payments
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());
