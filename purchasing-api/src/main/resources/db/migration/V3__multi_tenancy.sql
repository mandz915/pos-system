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


-- suppliers
ALTER TABLE suppliers ADD COLUMN tenant_id BIGINT;
UPDATE suppliers SET tenant_id = 1;
ALTER TABLE suppliers ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE suppliers ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_suppliers_tenant ON suppliers (tenant_id);
ALTER TABLE suppliers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON suppliers
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- purchase_orders
ALTER TABLE purchase_orders ADD COLUMN tenant_id BIGINT;
UPDATE purchase_orders SET tenant_id = 1;
ALTER TABLE purchase_orders ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE purchase_orders ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_purchase_orders_tenant ON purchase_orders (tenant_id);
ALTER TABLE purchase_orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON purchase_orders
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- po_lines
ALTER TABLE po_lines ADD COLUMN tenant_id BIGINT;
UPDATE po_lines SET tenant_id = 1;
ALTER TABLE po_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE po_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_po_lines_tenant ON po_lines (tenant_id);
ALTER TABLE po_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON po_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- goods_receipts
ALTER TABLE goods_receipts ADD COLUMN tenant_id BIGINT;
UPDATE goods_receipts SET tenant_id = 1;
ALTER TABLE goods_receipts ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE goods_receipts ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_goods_receipts_tenant ON goods_receipts (tenant_id);
ALTER TABLE goods_receipts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON goods_receipts
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- goods_receipt_lines
ALTER TABLE goods_receipt_lines ADD COLUMN tenant_id BIGINT;
UPDATE goods_receipt_lines SET tenant_id = 1;
ALTER TABLE goods_receipt_lines ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE goods_receipt_lines ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_goods_receipt_lines_tenant ON goods_receipt_lines (tenant_id);
ALTER TABLE goods_receipt_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON goods_receipt_lines
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

-- Codes and numbers are unique per tenant, not across the whole system.
ALTER TABLE suppliers DROP CONSTRAINT suppliers_code_key;
ALTER TABLE suppliers ADD CONSTRAINT suppliers_code_key UNIQUE (tenant_id, code);
ALTER TABLE purchase_orders DROP CONSTRAINT purchase_orders_po_number_key;
ALTER TABLE purchase_orders ADD CONSTRAINT purchase_orders_po_number_key UNIQUE (tenant_id, po_number);

-- The relay copies this into a Kafka header so consumers work for the right tenant.
-- No row-level security: the relay publishes every tenant's events.
ALTER TABLE outbox ADD COLUMN tenant_id BIGINT;
