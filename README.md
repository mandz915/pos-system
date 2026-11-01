# POS System

A multi-tenant, multi-store point-of-sale backend built as a set of Spring Boot services in one Maven monorepo, designed for cloud deployment. Many client companies share one deployment; each has its own stores, users, catalog and sales, and cannot see anybody else's.

**Stack:** Java 21 (virtual threads) · Spring Boot 4.1 · Spring Cloud Gateway · MyBatis · Flyway · PostgreSQL (one database per service) · Kafka · Redis · Lombok · Docker · Kubernetes · GitHub Actions

## Contents

- [Features](#features)
- [Services](#services)
- [Multi-tenancy: many clients, many stores](#multi-tenancy-many-clients-many-stores)
- [Architecture](#architecture)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Promotions and per-line tax](#promotions-and-per-line-tax)
- [Redis](#redis)
- [Throughput: 10,000 transactions per minute](#throughput-10000-transactions-per-minute)
- [Tests](#tests)
- [Deploy to the cloud](#deploy-to-the-cloud)
- [Before going to production](#before-going-to-production)
- [Repository layout](#repository-layout)
- [Project status](#project-status)

## Features

- Many client companies (tenants) on one deployment, each with any number of stores; data separated by PostgreSQL row-level security
- Sales with hold, resume and void, split payments, refunds, receipts, shifts, cash in/out and X/Z-read
- Catalog with categories, tax rates and promotions (percent, fixed, buy-X-get-Y, coupons, per-store, minimum quantity)
- Optional per-line tax override and an optional `tax` object on every product
- Inventory by store with movements, transfers, physical counts and low-stock alerts
- Customers with loyalty points and store credit; purchasing with purchase orders and goods receipts
- Reports built from events; notifications for low stock and goods received
- JWT authentication, role-based access, store scoping, audit trail, instant token revocation
- Event-driven integration through Kafka with a transactional outbox and idempotent consumers

## Services

| Service | Port | Responsibility |
|---|---|---|
| `gateway-api` | 8080 | Single entry point for the frontend. Routes `/api/**` to the services, validates JWTs, rate limits, CORS. |
| `user-api` | 8081 | Client companies (tenants), login (password and cashier PIN, by company code), logout, users, roles, audit trail. Issues and revokes JWTs. |
| `inventory-api` | 8082 | Stock levels per store, movements, transfers, physical counts, low-stock alerts. |
| `sales-api` | 8083 | Sales (hold, resume, void), pricing, checkout with split payments, refunds, receipts, shifts, cash in/out, X/Z-read. |
| `administration-api` | 8084 | Stores, terminals, settings (global defaults with per-store overrides). |
| `catalog-api` | 8085 | Products, categories, tax rates, promotions. Source of truth for prices. |
| `payment-api` | 8086 | Cash / card / e-wallet payments, refunds, reversals, reconciliation, pluggable payment gateway. |
| `customer-api` | 8087 | Customers, loyalty points, store credit (tab). |
| `purchasing-api` | 8088 | Suppliers, purchase orders, goods receipts. |
| `reporting-api` | 8089 | Sales summaries, top products, per-cashier totals, CSV export. Built from events. |
| `notification-api` | 8090 | Queues and dispatches low-stock / goods-received notifications. |
| `pos-common` | n/a | Shared library: JWT, tenant context and tenant-aware database connections, auth interceptor (`@RequireRole`), error handling, event contracts, transactional outbox, promotion engine, Redis cache and token blocklist. |
| `pos-test-support` | n/a | Test helper: `MockMvc` wired with the real JWT interceptor and error handler, for Mockito API tests. |

## Multi-tenancy: many clients, many stores

One deployment serves many **client companies** (tenants). Each company has any number of stores, its own users, catalog,
prices, customers, suppliers and sales. Companies cannot see or change each other's data, and their codes (SKU, barcode, store
code, coupon, username, ...) only have to be unique inside the company.

```
Platform operator (PLATFORM_ADMIN)  ──creates / suspends──>  Company (tenant)  ──has many──>  Stores ──> Terminals
                                                                  │
                                           ADMIN of the company manages its stores, users, catalog, settings
                                           MANAGER / SUPERVISOR / CASHIER work in one store (or head office)
```

**How the separation works (defence in depth)**

1. **The JWT carries the tenant** (`tenant` claim; `0` for platform operators). Every service takes the tenant from the
   verified token, never from a request parameter, so a caller cannot ask for another company.
2. **Every business table has a `tenant_id`** and PostgreSQL **row-level security**. Each time a connection leaves the pool it is
   told the caller's tenant (`TenantAwareDataSource`); the policies then hide every other company's rows and reject writes for
   another company. A query that forgets a filter still cannot leak, and with no tenant set a connection sees nothing.
3. **Inserts need no tenant parameter**: `tenant_id` defaults from the session, so the mappers stay unchanged.
4. **Two database roles.** The *owner* (`DB_OWNER_USER`) runs the Flyway migrations; the services connect as a plain *app role*
   (`DB_USER`, `pos_app`). A superuser or BYPASSRLS role would see everything, so the services **refuse to start** with one.
   `scripts/init-databases.sh` creates both roles for Docker Compose; create the same pair on a managed PostgreSQL.
5. **Events and caches are tenant-scoped too.** The outbox relay writes the tenant into a Kafka header, and a consumer
   interceptor restores it before the listener runs; Redis cache entries live under a per-tenant namespace, so one company's catalog
   change does not flush the others and one company can never be served another's cached data.

**Onboarding a client**

1. Log in as the platform operator (first start creates `platform` / `Platform#12345`; set `PLATFORM_PASSWORD`). Log in without
   a `tenant` field.
2. `POST /api/platform/tenants` with a company code, name and the first administrator. The company starts empty.
3. That administrator logs in with `"tenant": "<CODE>"`, creates stores (`/api/admin/stores`), terminals, tax rates, categories,
   products and users. Settings fall back to built-in defaults (currency, receipt text, VAT rate) until the company changes them.
4. `POST /api/platform/tenants/{id}/suspend` blocks logins and signs everybody out immediately; `/activate` reverses it.

Login requests take the company code:

```json
{ "tenant": "ACME", "username": "cora", "password": "..." }
```

The demo company `DEFAULT` (tenant 1) owns the seed data (store `MAIN`, tax rates, `General` category) and its administrator
`admin` / `Admin#12345`. Existing single-company data is assigned to it by the `V3__multi_tenancy.sql` migrations.

**Things to know**

- Ids (stores, products, ...) are global sequences, not per company, so a company's ids have gaps. PO numbers likewise.
- The platform role can create and suspend companies but owns no business data; it cannot read a company's products or sales.
- Company administrators cannot grant the platform role (it is not offered and is rejected).
- Prove the separation on your own database with `scripts/verify-tenant-isolation.sh` (it also detects a role that bypasses
  row-level security).
- Redis is shared across companies but every key is company-scoped. Rate limits are per user, so one company cannot exhaust
  another's allowance.

## Architecture

```
 Frontend / till ──> gateway-api ──> user | administration | catalog | inventory | sales | payment
                                       customer | purchasing | reporting | notification
 sales-api ──REST──> catalog-api (authoritative price/tax)   sales-api ──REST──> payment-api (charge/refund)

 every service ──> Redis: catalog cache, token blocklist     gateway ──> Redis: shared rate limits
 sales-api      ──outbox──> Kafka: pos.sale.completed, pos.sale.refunded ──> inventory, customer, reporting
 purchasing-api ──outbox──> Kafka: pos.goods.received                    ──> inventory, notification
 inventory-api  ──outbox──> Kafka: pos.inventory.low-stock               ──> notification
```

Design points worth knowing:

- **Transactional outbox.** A service writes its business change and the event in the same database transaction;
  a relay publishes to Kafka afterwards. State and events cannot diverge. Consumers are idempotent
  (`processed_events`), so redelivery never double-counts stock or points.
- **Idempotent sales.** The till sends a client generated `clientRef` (UUID) when creating a sale. Retrying after a
  network drop returns the same sale instead of creating a duplicate.
- **Price snapshots.** Sale lines store the price, discount and tax at the time of sale; later catalog changes never
  alter a recorded sale.
- **Defence in depth.** The gateway rejects bad tokens, and every service validates the JWT again and enforces
  roles and store scope. A cashier of store 3 cannot read store 4.
- **Stock never blocks a sale.** Sales deduct stock from the Kafka event and may take it below zero (a count or
  adjustment fixes the difference). Manual adjustments and transfers cannot go negative.
- **Checkout is compensating.** Payments are taken first; if completing the sale fails, approved payments are
  reversed. If a reversal itself fails, the log line `MANUAL ACTION REQUIRED` identifies the payment.

**Roles:** `ADMIN` (everything, all stores), `MANAGER`, `SUPERVISOR` (refunds, stock changes, reports), `CASHIER` (sell, own shift).
Users without a store (head office) can see all stores; users bound to a store only see their own.

## Quick start

Prerequisites: JDK 21, Maven 3.9+, Docker.

```bash
cp .env.example .env              # optional: change passwords / JWT secret
docker compose up -d              # PostgreSQL (databases and the two database roles are created) + Kafka + Redis
                                  # upgrading from an earlier version? run `docker compose down -v` first so the roles are created
mvn -q -DskipTests install        # build everything
```

Start the services from IntelliJ (run each `*ApiApplication` class once Maven is loaded)
or from a terminal, for example `mvn -pl user-api spring-boot:run`. Start at least `gateway-api`, `user-api`,
`administration-api`, `catalog-api`, `inventory-api`, `payment-api` and `sales-api` to sell something.

Or run everything in containers:

```bash
docker compose --profile apps up -d --build
```

Flyway creates the tables on first start. `user-api` creates two logins when it has no users:
the platform operator **`platform` / `Platform#12345`** (log in without a company; creates client companies) and the administrator
of the demo company `DEFAULT`, **`admin` / `Admin#12345`** (log in with `"tenant": "DEFAULT"`). Set `PLATFORM_PASSWORD` and
`ADMIN_PASSWORD`, and change both after logging in. The database seed gives the demo company store `MAIN` (id 1) with terminal
`MAIN-T1` (id 1), a `General` category, and the VAT 12% / zero-rated tax rates; a new company starts empty.

Open `http/pos.http` in IntelliJ and run the requests top to bottom: login, create a product, add stock, open a
shift, sell, pay, print the receipt, refund, X/Z-read, then promotions, the line tax object, logout and forced sign-out, and
finally the multi-company steps (create a company, set up its stores and catalog, see the isolation, suspend it).

## Configuration

| Variable | Default | Used by |
|---|---|---|
| `JWT_SECRET` | dev value | all (must be identical everywhere, 32+ characters) |
| `DB_URL` | localhost | every service with a database |
| `DB_USER`, `DB_PASSWORD` | `pos_app` / `pos_app` | the application role. Must be a plain role (not superuser, no BYPASSRLS) so tenant isolation applies; services refuse to start otherwise |
| `DB_OWNER_USER`, `DB_OWNER_PASSWORD` | `pos` / `pos` | database owner: runs Flyway (passed to services as `FLYWAY_USER`, `FLYWAY_PASSWORD`) and creates the roles in compose |
| `DB_POOL_SIZE` | `30` | every service (Hikari pool per instance) |
| `KAFKA_BOOTSTRAP` | `localhost:29092` | services that use Kafka |
| `KAFKA_CONCURRENCY` | `3` | consumer threads per listener (topics have 6 partitions) |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL` | `localhost` / `6379` / empty / `false` | every service (cache, rate limit, token blocklist) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | `admin` / `Admin#12345` | user-api: administrator of the demo company `DEFAULT` (first start only) |
| `PLATFORM_USERNAME`, `PLATFORM_PASSWORD` | `platform` / `Platform#12345` | user-api: platform operator who creates client companies (first start only) |
| `CORS_ALLOWED_ORIGINS` | `*` | gateway-api (set your frontend origin in production) |
| `RATE_LIMIT_PER_MINUTE` | `1200` | gateway-api, per signed-in user, shared across gateway instances through Redis |
| `RATE_LIMIT_ANONYMOUS_PER_MINUTE` | `60` | gateway-api, per client address for login / PIN login |
| `CATALOG_CACHE_TTL` | `30s` | catalog-api (upper bound of staleness if an eviction is missed) |
| `pos.tenancy.enforce-rls` | `true` | every service with a database. Set `false` only for a local experiment with a superuser role |
| `PAYMENT_GATEWAY` | `mock` | payment-api |
| `APP_TIMEZONE` | `Asia/Manila` | payment, reporting (day boundaries) |
| `OPS_EMAIL` | `ops@example.com` | notification-api |

## Promotions and per-line tax

**Promotions** live in `catalog-api` (`/api/catalog/promotions`, manager or admin) and are evaluated by `sales-api` for the
quantity actually bought, with the engine in `pos-common` (`PromotionEngine`):

| Field | Meaning |
|---|---|
| `type` | `PERCENT` (value = % off), `FIXED` (value = amount off per unit), `BUY_X_GET_Y` (`buyQty` paid, `getQty` of the same product free, repeating) |
| `productId` / `categoryId` | Target one product, one category, or everything when both are empty (`BUY_X_GET_Y` must target a product or category) |
| `minQty` | The line quantity must reach this to qualify |
| `storeId` | Only this store (empty = every store) |
| `couponCode` | Only when the cashier sends this code in `couponCodes` (empty = automatic) |
| `startsAt` / `endsAt` / `active` | Validity window |

Per line the single best promotion wins; promotions do not stack. The winning code is stored on the sale line
(`promotionCode`) so the receipt can always be explained. An unknown, expired or other-store coupon makes the sale
request fail with 400 instead of being silently ignored. Product lookups show a per-unit price preview
(`discountPerUnit`) for automatic percent/fixed promotions.

**Optional tax object on a sale line.** By default a line takes the tax of its catalog product. A supervisor, manager or admin
can override it for one line with an optional object (cashiers get 403):

```json
{ "productId": 5, "quantity": 1, "tax": { "code": "EXEMPT", "rate": 0, "inclusive": true } }
```

`inclusive` may be omitted to keep the product's setting. The override is recorded on the line (`taxCode`, `taxOverridden`).
Products expose their tax the same way, as one optional `tax` object (`id`, `code`, `rate`, `inclusive`; absent when the
product has no tax), next to the older flat `taxRate`/`taxInclusive` fields.

Prices are tax-inclusive by default: tax is the share `rate / (100 + rate)` of the net amount. Exclusive tax is added on top.
Amounts are rounded per line, so a receipt always adds up.

## Redis

Redis holds only disposable data, and every use degrades safely when Redis is slow or down (5 failures open a
5 second circuit, so an outage costs one fast failure per window rather than a timeout per request):

| Use | Where | Behaviour when Redis is down |
|---|---|---|
| **Catalog cache** | catalog-api caches product lookups (id, barcode, SKU) without cost; sales-api reads the same entries and skips an HTTP call per line, and caches the live promotions. Any catalog write evicts the whole namespace after commit (one `INCR`, O(1)). | Falls back to the database and to catalog-api |
| **Distributed rate limiting** | gateway-api: one counter per user per minute shared by all gateway instances; login is limited per address | In-memory limiter per instance |
| **Token blocklist** | `POST /api/auth/logout`, disabling an account, role/store change, password reset and `POST /api/users/{id}/sign-out` revoke tokens at once; checked by the gateway and by every service | Fails open: requests are served and revoked tokens work until they expire (8 hours at most) |

If Redis is restarted without persistence the blocklist is lost, so use a managed Redis with replication in production.
Set `pos.redis.enabled=false` to run without it.

## Throughput: 10,000 transactions per minute

10,000 per minute is about 167 completed sales per second (each is a create plus a checkout, so about 334 requests per
second through the gateway). What was done for it:

- **Virtual threads** (`spring.threads.virtual.enabled=true`) in every servlet service: a request blocked on the database
  or another service no longer holds a scarce platform thread, so concurrency is limited by the connection pools, not by
  Tomcat's thread count. Tomcat accepts up to 10,000 connections. The gateway is reactive and unchanged.
- **Connection pools** are explicit (`DB_POOL_SIZE`, 30 per instance) and Postgres is started with `max_connections=500` in
  compose. With many replicas put PgBouncer in front; the pool is what protects the database.
- **Shorter hot path:** catalog data comes from Redis, not from an HTTP call and a query per line, and the receipt-number
  row lock is taken last in the checkout transaction so it is held for milliseconds, not for the whole transaction.
- **Outbox relay** sends a batch of 500 asynchronously, marks it with one batched update and keeps draining. The first
  version published one event at a time and could not exceed roughly 75 events per second.
- **Kafka:** topics are created with 6 partitions (`pos.kafka.partitions`), consumers run 3 threads per listener, producers
  batch (`linger.ms=5`, lz4). Partition count can be raised later, never lowered.
- **Rate limits** are per user, not per IP, so a store whose tills share one public address is not throttled as one client.

**This has not been measured.** The code was written for the target but never run under load. Run
`loadtest/checkout.js` ([k6](https://k6.io)) against a production-like environment, watch Hikari pool wait, Kafka consumer
lag and p95 latency, then tune `DB_POOL_SIZE`, replicas and partitions. Payment is the likely limit once the mock gateway
is replaced by a real provider.

```bash
RATE_LIMIT_PER_MINUTE=200000 ...    # start the gateway with a high per-user limit (the script uses one token)
k6 run -e BASE=http://localhost:8080 loadtest/checkout.js
```

## Persistence (MyBatis, XML style)

SQL lives in XML mappers, not in annotations: each `mapper/XyzMapper.java` interface in a service has a matching
`src/main/resources/mapper/XyzMapper.xml` whose `namespace` is the interface and whose statement `id`s are the method names.
`mybatis.mapper-locations: classpath:mapper/*.xml` loads them, `resultType` names the entity (`domain` package), and
shared fragments use `<sql>` / `<include>`. Dynamic filters use `<where>` / `<if>`. Keep the interface method and the XML
statement name in sync; an unmatched method fails at the first call with "Invalid bound statement".

## Tests

```bash
mvn verify                          # unit + Mockito API tests of every module
mvn -pl sales-api -am test          # one service (and what it depends on)
```

- **Mockito API tests** use `pos-test-support`: a standalone `MockMvc` around ONE controller whose service is a Mockito mock,
  behind the real JWT interceptor and the real error handler. They check status codes, JSON bodies, validation, roles
  (401/403) and revoked tokens with genuinely signed tokens, without starting Spring, so they run in milliseconds.
  Examples: `AuthControllerTest`, `CatalogControllerTest`, `SaleControllerTest`, gateway `JwtAuthFilterTest`.
- **Service tests with Mockito** mock mappers and clients: `SaleServiceTest` (promotions, coupons, tax override, payment
  reversal on failure), `CatalogServiceTest` (cache hit/miss/eviction), `UserServiceTest` and `AuthServiceTest` (token
  revocation), `RedisJsonCacheTest`, `RedisTokenRevocationsTest`, `OutboxRelayTest`.
- **Tenancy tests:** `TenantContextTest`, `TenantAwareDataSourceTest` (the connection is told the tenant on every checkout),
  `TenantRecordInterceptorTest`, tenant-scoped cache tests, `TenantControllerTest` (platform-only access), `TenantServiceTest`
  (company creation, rollback, suspension) and login tests for unknown and suspended companies. Row-level security itself needs
  a database: run `scripts/verify-tenant-isolation.sh`.
- **Pure logic tests** (`PromotionEngineTest`, `PricingCalculatorTest`, `TenderPlannerTest`, ...) need no mocks.

There are no end-to-end tests against real PostgreSQL, Kafka and Redis yet (Testcontainers would be the next step).

## Deploy to the cloud

- `Dockerfile` builds any module: `docker build --build-arg MODULE=sales-api -t pos/sales-api .`
- `.github/workflows/ci.yml` runs `mvn verify` on every push and pull request and, on `main`, pushes one image per
  service to GitHub Container Registry (`ghcr.io/<owner>/<repo>/<service>`).
- `deploy/k8s/pos-system.yaml` is a Kubernetes starting point (Deployments, Services, probes, ConfigMap/Secret,
  Ingress for the gateway, a single-node Redis for development). Use managed PostgreSQL (one database per service),
  managed Kafka and managed Redis, replace the placeholder secrets, and put TLS on the ingress.
- Health endpoints: `/actuator/health/liveness` and `/actuator/health/readiness` on every service.

## Before going to production

These are deliberately left for you, because they depend on your environment or on local regulations:

- **Tax and receipt compliance.** The receipt is a plain-text template. If you must issue BIR-accredited receipts or
  use a CAS/POS machine accreditation, the receipt format, numbering and reports must follow those rules.
- **Real payment provider.** `MockPaymentGateway` approves everything (a reference of `DECLINE` declines). Implement
  `PaymentGateway` for your acquirer or e-wallet and set `PAYMENT_GATEWAY`. Card data should never touch these services
  (use the provider's hosted fields or terminal).
- **Real email/SMS.** `LogNotificationSender` only logs. Implement `NotificationSender` for your provider.
- **Offline tills.** The API is built for it (idempotent `clientRef`) but the till client must queue and replay.
- **Database roles.** Create the owner and app roles on your managed PostgreSQL the way `scripts/init-databases.sh` does
  (app role: `NOSUPERUSER NOBYPASSRLS`, default privileges on the owner's tables), and give the services `DB_USER` (app) and
  `FLYWAY_USER` (owner). Managed services that make the master user a member of `rds_superuser` or similar need a separate plain role.
- **Per-company limits.** Rate limits are per user; there is no per-company quota, billing or usage metering yet.
- **Secrets:** replace the dev `JWT_SECRET` and database passwords, and use your cloud secret manager.
- **Hardware** (printers, scanners, cash drawers) is handled client-side or by a local agent, not by these services.
- **Not included:** inventory valuation reporting and cart-level promotions such as "spend 1,000, get 100 off".

## Repository layout

```
pos-system/
├── pom.xml                      parent POM (modules, dependency management, Lombok processor)
├── pos-common/                  shared library (JWT, outbox, promotion engine, Redis cache, error handling)
├── pos-test-support/            MockMvc + JWT helpers for Mockito API tests
├── gateway-api/ … notification-api/   the services (each: src/main/java with controller / dto / service / mapper / domain,
│                                resources/mapper/*.xml = MyBatis SQL, resources/db/migration, tests)
├── http/                        IntelliJ HTTP Client walkthrough (pos.http) and environments
├── loadtest/                    k6 load test (checkout.js)
├── deploy/k8s/                  Kubernetes manifest
├── scripts/                     database + role initialisation for compose, verify-tenant-isolation.sh
├── docker-compose.yml           Postgres, Kafka, Redis (+ services under --profile apps)
├── Dockerfile                   multi-stage build for any module
└── .github/workflows/ci.yml     build, test, push images
```

## Project status

First complete generation of the system. The database migrations (including row-level security and the roles), the mapper SQL and the pure-logic tests were checked
against a real PostgreSQL and a stand-in test runner, but a full Maven build, the Mockito tests, and the Spring, Kafka,
Redis, JWT and tenant-isolation wiring in the services have not been run end to end. Run `mvn clean verify` first and fix whatever it reports; the Spring
Boot 4 Redis auto-configuration class names are the likeliest place to need a correction.
