#!/bin/bash
# Proves, against YOUR migrated catalog database, that one company cannot see or change another's rows.
# It connects as the application role (the same one the services use), writes two throw-away companies
# (tenant ids 9001 and 9002), checks the isolation, and removes its test rows again with the owner role.
#
#   PGHOST=localhost DB=pos_catalog APP_DB_USER=pos_app APP_DB_PASSWORD=pos_app \
#   OWNER_DB_USER=pos OWNER_DB_PASSWORD=pos ./scripts/verify-tenant-isolation.sh
set -u
export PGHOST="${PGHOST:-localhost}"
DB="${DB:-pos_catalog}"
APP_USER="${APP_DB_USER:-pos_app}"; APP_PW="${APP_DB_PASSWORD:-pos_app}"
OWNER="${OWNER_DB_USER:-pos}"; OWNER_PW="${OWNER_DB_PASSWORD:-pos}"
fail=0

app() { PGPASSWORD="$APP_PW" psql -U "$APP_USER" -d "$DB" -q -X -t -A "$@" 2>&1; }
owner() { PGPASSWORD="$OWNER_PW" psql -U "$OWNER" -d "$DB" -q -X -t -A "$@" 2>&1; }
check() { # description, expected, actual
  if [ "$2" == "$3" ]; then echo "  ok   $1"; else echo "  FAIL $1 (expected '$2', got '$3')"; fail=1; fi
}

echo "Database role '$APP_USER' on $DB:"
check "role is not a superuser and has no BYPASSRLS" "f" "$(app -c "SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = current_user")"

cleanup() { owner -c "DELETE FROM products WHERE tenant_id IN (9001, 9002)" -c "DELETE FROM categories WHERE tenant_id IN (9001, 9002)" >/dev/null; }
cleanup

# one session per step, because the tenant is a session setting
app -c "SELECT set_config('app.tenant_id','9001',false)" \
    -c "INSERT INTO categories (name) VALUES ('Isolation test')" \
    -c "INSERT INTO products (sku, name, category_id, price) SELECT 'ISO-1','Test product',id,10 FROM categories WHERE name='Isolation test'" >/dev/null
check "tenant 9001 sees its own product" "1" "$(app -c "SELECT set_config('app.tenant_id','9001',false)" -c "SELECT count(*) FROM products WHERE sku='ISO-1'" | tail -1)"
check "tenant 9002 does not see it" "0" "$(app -c "SELECT set_config('app.tenant_id','9002',false)" -c "SELECT count(*) FROM products WHERE sku='ISO-1'" | tail -1)"
check "a session with no tenant sees nothing" "0" "$(app -c "SELECT set_config('app.tenant_id','',false)" -c "SELECT count(*) FROM products" | tail -1)"
check "tenant 9002 cannot update it" "0" "$(app -c "SELECT set_config('app.tenant_id','9002',false)" -c "WITH u AS (UPDATE products SET price = 1 WHERE sku='ISO-1' RETURNING 1) SELECT count(*) FROM u" | tail -1)"
out="$(app -c "SELECT set_config('app.tenant_id','9002',false)" -c "INSERT INTO categories (name, tenant_id) VALUES ('forged', 9001)")"
check "tenant 9002 cannot write a row for 9001" "yes" "$(echo "$out" | grep -q 'row-level security' && echo yes || echo no)"
out="$(app -c "SELECT set_config('app.tenant_id','9002',false)" -c "INSERT INTO categories (name) VALUES ('Isolation test')" -c "INSERT INTO products (sku, name, category_id, price) SELECT 'ISO-1','Same sku',id,11 FROM categories WHERE name='Isolation test'")"
check "tenant 9002 may reuse tenant 9001's SKU" "no" "$(echo "$out" | grep -qi 'duplicate key' && echo yes || echo no)"

cleanup
if [ $fail -eq 0 ]; then echo "Tenant isolation verified."; else echo "TENANT ISOLATION IS NOT WORKING"; fi
exit $fail
