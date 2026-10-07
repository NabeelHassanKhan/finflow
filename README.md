# FinFlow - Digital Banking Backend API

[![CI](https://github.com/NabeelHassanKhan/finflow/actions/workflows/ci.yml/badge.svg)](https://github.com/NabeelHassanKhan/finflow/actions/workflows/ci.yml)

A Spring Boot backend for digital banking: user registration with JWT authentication, account management, account-to-account fund transfers, and transaction history. Built to demonstrate production-style practices: transactional integrity, concurrency control, caching, and clean layered architecture.

## Tech Stack

| Area | Technology |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.4.5 |
| Database | MySQL (main), H2 (tests only) |
| Cache | Redis |
| Security | Spring Security 6 + JWT (jjwt 0.12.6) |
| Mapping / Boilerplate | MapStruct, Lombok |
| API Docs | Swagger / OpenAPI 3 (springdoc 2.8.6) |
| Testing | JUnit 5, Mockito |
| Build | Maven |

## Features

- **Authentication:** user registration and login with JWT (stateless, Spring Security 6)
- **Accounts:** open SAVINGS or CURRENT accounts, view account details, balance enquiry
- **Fund transfers:** account-to-account transfers with `@Transactional` (all-or-nothing)
- **Transaction history:** filters, pagination and date range, plus mini statement (last N transactions)
- **Daily transfer limit:** configurable business rule (`finflow.transfer.daily-limit`)
- **Concurrency control:** optimistic locking with retry on conflicting transfers
- **Caching:** Redis caching for account and balance data
- **Error handling:** global exception handler with a standard `ApiResponse` wrapper
- **API docs:** Swagger UI with JWT Authorize button

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8+
- Docker (for Redis)

### 1. Clone the repository

```bash
git clone https://github.com/NabeelHassanKhan/finflow.git
cd finflow
```

### 2. Create the MySQL database

```sql
CREATE DATABASE finflow;
```

Tables are created automatically on first run (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Start Redis

```bash
docker run -d --name finflow-redis -p 6379:6379 redis:7
```

### 4. Set environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | Yes | none | Secret key for signing JWTs (minimum 32 bytes) |
| `DB_PASSWORD` | Yes | none | MySQL password |
| `DB_USERNAME` | No | `root` | MySQL username |
| `DB_URL` | No | `jdbc:mysql://localhost:3306/finflow?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | JDBC connection URL |
| `REDIS_HOST` | No | `localhost` | Redis host |
| `REDIS_PORT` | No | `6379` | Redis port |

The app will not start if `JWT_SECRET` or `DB_PASSWORD` is missing (fail-fast by design).

Generate a strong secret and export the variables:

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export DB_PASSWORD=your_mysql_password
```

### 5. Run the application

```bash
./mvnw spring-boot:run
```

(or `mvn spring-boot:run` if you use a local Maven install)

### 6. Open Swagger UI

http://localhost:8080/swagger-ui/index.html

Register a user, log in, click **Authorize**, paste the token, and try the protected endpoints.

## API Endpoints

Full interactive documentation is available in Swagger UI (`/swagger-ui/index.html`). Endpoints under `/api/auth/**` are public; all others require a JWT in the `Authorization: Bearer <token>` header.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Log in and receive a JWT |
| POST | `/api/accounts/open` | JWT | Open a SAVINGS or CURRENT account |
| GET | `/api/accounts/{accountNumber}` | JWT | Get account details |
| GET | `/api/accounts/{accountNumber}/balance` | JWT | Balance enquiry |
| GET | `/api/accounts/my-accounts` | JWT | List the logged-in user's accounts |
| POST | `/api/transfers` | JWT | Transfer funds between accounts |
| GET | `/api/transactions/{accountNumber}` | JWT | Transaction history (filters + pagination) |

### Transaction history query parameters

| Param | Type | Description |
|---|---|---|
| `type` | `CREDIT` / `DEBIT` / `TRANSFER` | Filter by transaction type (optional) |
| `startDate` | `yyyy-MM-dd` | Start of date range (optional) |
| `endDate` | `yyyy-MM-dd` | End of date range (optional) |
| `page` | integer | Page number, starts at 0 (default `0`) |
| `size` | integer | Page size (default `10`) |
| `sort` | string | Sort field and direction (default `createdAt,desc`) |

**Mini statement:** there is no separate endpoint. Use the history endpoint with a small page size, for example `GET /api/transactions/{accountNumber}?size=5` returns the last 5 transactions (newest first).