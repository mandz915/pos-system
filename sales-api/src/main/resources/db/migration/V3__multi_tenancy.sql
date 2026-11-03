CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;

ALTER TABLE shifts ADD COLUMN tenant_id BIGINT;
UPDATE shifts SET tenant_id = 1;
ALTER TABLE shifts ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE shifts ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_shifts_tenant ON shifts (tenant_id);
ALTER TABLE shifts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON shifts
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE cash_movements ADD COLUMN tenant_id BIGINT;
UPDATE cash_movements SET tenant_id = 1;
ALTER TABLE cash_movements ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE cash_movements ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_cash_movements_tenant ON cash_movements (tenant_id);
ALTER TABLE cash_movements ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON cash_movements
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE receipt_sequences ADD COLUMN tenant_id BIGINT;
UPDATE receipt_sequences SET tenant_id = 1;
ALTER TABLE receipt_sequences ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE receipt_sequences ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_receipt_sequences_tenant ON receipt_sequences (tenant_id);
ALTER TABLE receipt_sequences ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON receipt_sequences
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE sales ADD COLUMN tenant_id BIGINT;
UPDATE sales SET tenant_id = 1;
ALTER TABLE sales ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sales ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sales_tenant ON sales (tenant_id);
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sales
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE sale_lines ADD COLUMN tenant_id BIGINT;
UPDATE sale_lines SET tenant_id = 1;
ALTER TABLE sale_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_lines_tenant ON sale_lines (tenant_id);
ALTER TABLE sale_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE sale_payments ADD COLUMN tenant_id BIGINT;
UPDATE sale_payments SET tenant_id = 1;
ALTER TABLE sale_payments ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_payments ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_payments_tenant ON sale_payments (tenant_id);
ALTER TABLE sale_payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_payments
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE sale_returns ADD COLUMN tenant_id BIGINT;
UPDATE sale_returns SET tenant_id = 1;
ALTER TABLE sale_returns ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_returns ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_returns_tenant ON sale_returns (tenant_id);
ALTER TABLE sale_returns ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_returns
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE sale_return_lines ADD COLUMN tenant_id BIGINT;
UPDATE sale_return_lines SET tenant_id = 1;
ALTER TABLE sale_return_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE sale_return_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_sale_return_lines_tenant ON sale_return_lines (tenant_id);
ALTER TABLE sale_return_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sale_return_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE sales DROP CONSTRAINT sales_client_ref_key;
ALTER TABLE sales ADD CONSTRAINT sales_client_ref_key UNIQUE (tenant_id, client_ref);
ALTER TABLE sales DROP CONSTRAINT sales_receipt_no_key;
ALTER TABLE sales ADD CONSTRAINT sales_receipt_no_key UNIQUE (tenant_id, receipt_no);

ALTER TABLE outbox ADD COLUMN tenant_id BIGINT;
