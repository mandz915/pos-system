CREATE FUNCTION pos_current_tenant() RETURNS BIGINT
    LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::bigint $$;

CREATE FUNCTION pos_system_mode() RETURNS BOOLEAN
    LANGUAGE sql STABLE AS
$$ SELECT coalesce(current_setting('app.system', true), 'off') = 'on' $$;

CREATE TABLE tenants (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(30)  NOT NULL UNIQUE,
    name       VARCHAR(150) NOT NULL,
    status     VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (id > 0)
);

INSERT INTO tenants (id, code, name) VALUES (1, 'DEFAULT', 'Default Company');
SELECT setval(pg_get_serial_sequence('tenants', 'id'), 1);

INSERT INTO roles (name) VALUES ('PLATFORM_ADMIN');

ALTER TABLE users ADD COLUMN tenant_id BIGINT;
UPDATE users SET tenant_id = 1;
ALTER TABLE users ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_users_tenant ON users (tenant_id);
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON users
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE user_roles ADD COLUMN tenant_id BIGINT;
UPDATE user_roles SET tenant_id = 1;
ALTER TABLE user_roles ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE user_roles ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_user_roles_tenant ON user_roles (tenant_id);
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON user_roles
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

ALTER TABLE audit_log ADD COLUMN tenant_id BIGINT;
UPDATE audit_log SET tenant_id = 1;
ALTER TABLE audit_log ALTER COLUMN tenant_id SET DEFAULT pos_current_tenant();
ALTER TABLE audit_log ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_audit_log_tenant ON audit_log (tenant_id);
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON audit_log
    USING (tenant_id = pos_current_tenant())
    WITH CHECK (tenant_id = pos_current_tenant());

DROP INDEX uq_users_username;
CREATE UNIQUE INDEX uq_users_username ON users (tenant_id, lower(username));
