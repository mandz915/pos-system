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


-- customers
ALTER TABLE customers ADD COLUMN tenant_id BIGINT;
UPDATE customers SET tenant_id = 1;
ALTER TABLE customers ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE customers ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_customers_tenant ON customers (tenant_id);
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON customers
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- loyalty_transactions
ALTER TABLE loyalty_transactions ADD COLUMN tenant_id BIGINT;
UPDATE loyalty_transactions SET tenant_id = 1;
ALTER TABLE loyalty_transactions ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE loyalty_transactions ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_loyalty_transactions_tenant ON loyalty_transactions (tenant_id);
ALTER TABLE loyalty_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON loyalty_transactions
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- credit_transactions
ALTER TABLE credit_transactions ADD COLUMN tenant_id BIGINT;
UPDATE credit_transactions SET tenant_id = 1;
ALTER TABLE credit_transactions ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE credit_transactions ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_credit_transactions_tenant ON credit_transactions (tenant_id);
ALTER TABLE credit_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON credit_transactions
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- Codes and numbers are unique per tenant, not across the whole system.
ALTER TABLE customers DROP CONSTRAINT customers_code_key;
ALTER TABLE customers ADD CONSTRAINT customers_code_key UNIQUE (tenant_id, code);
