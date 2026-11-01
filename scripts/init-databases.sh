#!/bin/bash
# Executed once by the postgres container (docker-entrypoint-initdb.d), as the owner role (POSTGRES_USER).
#
# Two roles are used on purpose:
#   * the OWNER ($POSTGRES_USER) owns the databases and runs the Flyway migrations;
#   * the APP role ($APP_DB_USER) is what the services connect as. It is a plain role, so the row-level security
#     policies that separate the client companies (tenants) apply to it. A superuser or BYPASSRLS role would
#     silently see every company's data, and the services refuse to start with one.
set -e

APP_USER="${APP_DB_USER:-pos_app}"
APP_PASSWORD="${APP_DB_PASSWORD:-pos_app}"
DATABASES="pos_user pos_inventory pos_sales pos_administration pos_catalog pos_payment pos_customer pos_purchasing pos_reporting pos_notification"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
	CREATE ROLE "$APP_USER" LOGIN PASSWORD '$APP_PASSWORD' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
SQL

for db in $DATABASES; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE DATABASE $db OWNER \"$POSTGRES_USER\""
  # Tables the owner creates later (Flyway) are automatically usable by the app role.
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" <<-SQL
	GRANT CONNECT ON DATABASE $db TO "$APP_USER";
	GRANT USAGE ON SCHEMA public TO "$APP_USER";
	ALTER DEFAULT PRIVILEGES FOR ROLE "$POSTGRES_USER" IN SCHEMA public
	    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "$APP_USER";
	ALTER DEFAULT PRIVILEGES FOR ROLE "$POSTGRES_USER" IN SCHEMA public
	    GRANT USAGE, SELECT ON SEQUENCES TO "$APP_USER";
SQL
done
