# POS System

A multi-store point-of-sale backend built as a set of Spring Boot services in a single Maven monorepo.
Java 21, Spring Boot 4.1, MyBatis, PostgreSQL (one database per service), Kafka, Docker.

## Services

| Service | Port | Responsibility |
|---|---|---|
| `gateway-api` | 8080 | Single entry point for the frontend. Routes `/api/**` to the services, validates JWTs, rate limits, CORS. |
| `user-api` | 8081 | Login (password and cashier PIN), users, roles, audit trail. Issues JWTs. |
| `inventory-api` | 8082 | Stock levels per store, movements, transfers, physical counts, low-stock alerts. |
| `sales-api` | 8083 | Sales (hold, resume, void), checkout with split payments, refunds, receipts, shifts, cash in/out, X/Z-read. |
| `administration-api` | 8084 | Stores, terminals, settings (global defaults with per-store overrides). |
| `catalog-api` | 8085 | Products, categories, tax rates, promotions. Source of truth for prices. |
| `payment-api` | 8086 | Cash / card / e-wallet payments, refunds, reversals, reconciliation, pluggable payment gateway. |
| `customer-api` | 8087 | Customers, loyalty points, store credit (tab). |
| `purchasing-api` | 8088 | Suppliers, purchase orders, goods receipts. |
| `reporting-api` | 8089 | Sales summaries, top products, per-cashier totals, CSV export. Built from events. |
| `notification-api` | 8090 | Queues and dispatches low-stock / goods-received notifications. |
| `pos-common` | n/a | Shared library: JWT, auth interceptor (`@RequireRole`), error handling, event contracts, transactional outbox. |

## How it fits together

```
 Frontend / till ──> gateway-api ──> user | administration | catalog | inventory | sales | payment
                                       customer | purchasing | reporting | notification
 sales-api ──REST──> catalog-api (authoritative price/tax)   sales-api ──REST──> payment-api (charge/refund)

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

## Roles

`ADMIN` (everything, all stores), `MANAGER`, `SUPERVISOR` (refunds, stock changes, reports), `CASHIER` (sell, own shift).
Users without a store (head office) can see all stores; users bound to a store only see their own.

## Run it locally

Prerequisites: JDK 21, Maven 3.9+, Docker.

```bash
cp .env.example .env              # optional: change passwords / JWT secret
docker compose up -d              # PostgreSQL (all databases are created for you) + Kafka
mvn -q -DskipTests install        # build everything
```

Start the services from IntelliJ (run each `*ApiApplication` class once Maven is loaded)
or from a terminal, for example `mvn -pl user-api spring-boot:run`. Start at least `gateway-api`, `user-api`,
`administration-api`, `catalog-api`, `inventory-api`, `payment-api` and `sales-api` to sell something.

Or run everything in containers:

```bash
docker compose --profile apps up -d --build
```

Flyway creates the tables on first start. `user-api` creates the first administrator when it has no users:
**`admin` / `Admin#12345`** (set `ADMIN_PASSWORD` and change it after logging in). The database seed also creates
store `MAIN` (id 1) with terminal `MAIN-T1` (id 1), a `General` category, and the VAT 12% / zero-rated tax rates.

Open `http/pos.http` in IntelliJ and run the requests top to bottom: login, create a product, add stock, open a
shift, sell, pay, print the receipt, refund, X/Z-read.

## Configuration

| Variable | Default | Used by |
|---|---|---|
| `JWT_SECRET` | dev value | all (must be identical everywhere, 32+ characters) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | localhost / `pos` | every service with a database |
| `KAFKA_BOOTSTRAP` | `localhost:29092` | services that use Kafka |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | `admin` / `Admin#12345` | user-api (first start only) |
| `CORS_ALLOWED_ORIGINS` | `*` | gateway-api (set your frontend origin in production) |
| `RATE_LIMIT_PER_MINUTE` | `600` | gateway-api (per client address, per instance) |
| `PAYMENT_GATEWAY` | `mock` | payment-api |
| `APP_TIMEZONE` | `Asia/Manila` | payment, reporting (day boundaries) |
| `OPS_EMAIL` | `ops@example.com` | notification-api |

## Deploy to the cloud

- `Dockerfile` builds any module: `docker build --build-arg MODULE=sales-api -t pos/sales-api .`
- `.github/workflows/ci.yml` runs `mvn verify` on every push and pull request and, on `main`, pushes one image per
  service to GitHub Container Registry (`ghcr.io/<owner>/<repo>/<service>`).
- `deploy/k8s/pos-system.yaml` is a Kubernetes starting point (Deployments, Services, probes, ConfigMap/Secret,
  Ingress for the gateway). Use managed PostgreSQL (one database per service) and managed Kafka, replace the
  placeholder secrets, and put TLS on the ingress.
- Health endpoints: `/actuator/health/liveness` and `/actuator/health/readiness` on every service.

## Before going to production

These are deliberately left for you, because they depend on your environment or on local regulations:

- **Tax and receipt compliance.** The receipt is a plain-text template. If you must issue BIR-accredited receipts or
  use a CAS/POS machine accreditation, the receipt format, numbering and reports must follow those rules.
- **Real payment provider.** `MockPaymentGateway` approves everything. Implement `PaymentGateway` for your acquirer or
  e-wallet and set `PAYMENT_GATEWAY`. Card data should never touch these services (use the provider's hosted fields or
  terminal).
- **Real email/SMS.** `LogNotificationSender` only logs. Implement `NotificationSender` for your provider.
- **Offline tills.** The API is built for it (idempotent `clientRef`) but the till client must queue and replay.
- **Rate limiting** is in memory per gateway instance; use a shared store if you need exact cluster-wide quotas.
- **Secrets**: replace the dev `JWT_SECRET` and database passwords, use your cloud secret manager.
- **Hardware** (printers, scanners, cash drawers) is handled client-side or by a local agent, not by these services.
