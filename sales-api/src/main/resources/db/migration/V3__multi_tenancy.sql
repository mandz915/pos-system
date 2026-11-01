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


-- shifts
ALTER TABLE shifts ADD COLUMN tenant_id BIGINT;
UPDATE shifts SET tenant_id = 1;
ALTER TABLE shifts ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE shifts ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_shifts_tenant ON shifts (tenant_id);
ALTER TABLE shifts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON shifts
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- cash_movements
ALTER TABLE cash_movements ADD COLUMN tenant_id BIGINT;
UPDATE cash_movements SET tenant_id = 1;
ALTER TABLE cash_movements ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE cash_movements ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_cash_movements_tenant ON cash_movements (tenant_id);
ALTER TABLE cash_movements ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON cash_movements
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- receipt_sequences
ALTER TABLE receipt_sequences ADD COLUMN tenant_id BIGINT;
UPDATE receipt_sequences SET tenant_id = 1;
ALTER TABLE receipt_sequences ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE receipt_sequences ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_receipt_sequences_tenant ON receipt_sequences (tenant_id);
ALTER TABLE receipt_sequences ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON receipt_sequences
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- sales
ALTER TABLE sales ADD COLUMN tenant_id BIGINT;
UPDATE sales SET tenant_id = 1;
ALTER TABLE sales ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sales ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sales_tenant ON sales (tenant_id);
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sales
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- sale_lines
ALTER TABLE sale_lines ADD COLUMN tenant_id BIGINT;
UPDATE sale_lines SET tenant_id = 1;
ALTER TABLE sale_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_lines_tenant ON sale_lines (tenant_id);
ALTER TABLE sale_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- sale_payments
ALTER TABLE sale_payments ADD COLUMN tenant_id BIGINT;
UPDATE sale_payments SET tenant_id = 1;
ALTER TABLE sale_payments ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_payments ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_payments_tenant ON sale_payments (tenant_id);
ALTER TABLE sale_payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_payments
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- sale_returns
ALTER TABLE sale_returns ADD COLUMN tenant_id BIGINT;
UPDATE sale_returns SET tenant_id = 1;
ALTER TABLE sale_returns ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_returns ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_returns_tenant ON sale_returns (tenant_id);
ALTER TABLE sale_returns ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_returns
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- sale_return_lines
ALTER TABLE sale_return_lines ADD COLUMN tenant_id BIGINT;
UPDATE sale_return_lines SET tenant_id = 1;
ALTER TABLE sale_return_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_return_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_return_lines_tenant ON sale_return_lines (tenant_id);
ALTER TABLE sale_return_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_return_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- Codes and numbers are unique per tenant, not across the whole system.
ALTER TABLE sales DROP CONSTRAINT sales_client_ref_key;
ALTER TABLE sales ADD CONSTRAINT sales_client_ref_key UNIQUE (tenant_id, client_ref);
ALTER TABLE sales DROP CONSTRAINT sales_receipt_no_key;
ALTER TABLE sales ADD CONSTRAINT sales_receipt_no_key UNIQUE (tenant_id, receipt_no);

-- The relay copies this into a Kafka header so consumers work for the right tenant.
-- No row-level security: the relay publishes every tenant's events.
ALTER TABLE outbox ADD COLUMN tenant_id BIGINT;
