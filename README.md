# POS System

A multi-store point-of-sale backend built as a set of Spring Boot services in a single monorepo.

## Services

| Service | Port (planned) | Responsibility |
|---|---|---|
| `gateway-api` | 8080 | Single entry point for the frontend. Routes requests to the other services, validates JWTs, applies rate limiting. |
| `user-api` | 8081 | Login and user management across stores. Issues JWTs. |
| `inventory-api` | 8082 | Products and stock levels per store. |
| `sales-api` | 8083 | Sales transactions. Publishes sale events to Kafka. |
| `administration-api` | 8084 | System administration: stores, terminals, configuration, reports. |

## Tech Stack

- Java 21 or newer (Java 17 is the minimum for Spring Boot 4)
- Spring Boot 4.1.x
- Spring Cloud Gateway (gateway-api)
- MyBatis (data access)
- PostgreSQL (one database per service)
- Redis (caching, rate limiting, idempotency keys)
- Apache Kafka (events between services)
- Flyway (database migrations)
- Docker Compose (local infrastructure)

## Repository Layout

```
pos-system/
├── gateway-api/
├── user-api/
├── inventory-api/
├── sales-api/
├── administration-api/
├── docker-compose.yml        # PostgreSQL, Redis, Kafka for local development
├── .github/workflows/        # CI
├── .gitignore
└── README.md
```

## Design Notes

- Each service owns its own database. Services never read another service's tables.
- `user-api` issues JWTs that carry `storeId` and role claims. `gateway-api` validates them, and each service filters data by store.
- `sales-api` publishes a `sale.completed` event. `inventory-api` consumes it to reduce stock.
- Secrets live in environment variables, never in this repository.

## Getting Started

Setup steps will be added as the services are created.

1. Install JDK, Docker Desktop, and Git.
2. Start local infrastructure: `docker compose up -d`
3. Run each service from IntelliJ or with the build tool.

## Roadmap

- [ ] Generate the five service modules
- [ ] Add docker-compose for PostgreSQL, Redis, and Kafka
- [ ] user-api: login and JWT
- [ ] gateway-api: routing and token validation
- [ ] inventory-api: products and stock
- [ ] sales-api: transactions and Kafka events
- [ ] administration-api: stores, terminals, reports
- [ ] CI with GitHub Actions
