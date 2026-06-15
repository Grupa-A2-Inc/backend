# eLearning Backend

Spring Boot backend for a multi-tenant e-learning platform. The service exposes REST APIs for authentication, organization and user management, course authoring, classroom management, enrollments, assessments, analytics, feedback, subscriptions, and AI-assisted learning workflows.

## Table of Contents

- [Overview](#overview)
- [Core Capabilities](#core-capabilities)
- [Tech Stack](#tech-stack)
- [Architecture and Modules](#architecture-and-modules)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Authentication and Security](#authentication-and-security)
- [Database and Migrations](#database-and-migrations)
- [Testing](#testing)
- [Logging and Operations](#logging-and-operations)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)

## Overview

This project is the backend API for an e-learning system with:

- multi-tenant organization support
- role-based access control
- course, chapter, lesson, and resource management
- classroom membership and course assignment
- enrollments and progress tracking
- test authoring, attempts, grading, and results
- lesson ratings and question error reporting
- analytics and failure-rate monitoring
- Stripe-backed subscription management
- AI-generated tests and adaptive learning sessions
- email-based account activation and password reset flows

The codebase is organized by business domain under `src/main/java/org/elearning/backend` and uses Spring Security method authorization to enforce platform-wide and organization-scoped permissions.

## Core Capabilities

### Identity and access

- Public registration and login endpoints
- JWT access tokens for API authorization
- Refresh-token rotation through an `HttpOnly` cookie
- CSRF protection for cookie-authenticated endpoints
- Access-token blacklisting on logout
- Role model: `ADMIN`, `ORGANIZATION_ADMIN`, `TEACHER`, `STUDENT`, `PARENT`

### Learning platform domains

- Organizations and tenant administration
- User management, including bulk import and CSV import
- Courses, chapters, lessons, and lesson resources
- Classrooms, memberships, and classroom-course links
- Student enrollments and lesson progress
- Test creation, publication, attempts, and grading
- Course-completion certificate generation as PDF
- Lesson ratings and question issue reporting
- Analytics dashboards and failure-rate alerts

### External integrations

- PostgreSQL for persistence
- Flyway for schema migrations and seed data
- SMTP for password reset and account activation emails
- Stripe Checkout and webhooks for subscription workflows
- External AI service for question generation, adaptive exercises, feedback sync, and curriculum catalog access

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.4.3 |
| Build | Maven |
| Web/API | Spring Web, springdoc OpenAPI |
| Security | Spring Security, JJWT |
| Persistence | Spring Data JPA, PostgreSQL |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Mapping | MapStruct |
| Mail | Spring Mail |
| Payments | Stripe Java SDK |
| PDF | OpenPDF |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test, Mockito, Testcontainers dependencies |
| Utilities | Lombok, libphonenumber |

## Architecture and Modules

The service follows a conventional layered Spring architecture:

1. Controllers expose REST endpoints.
2. Services implement business logic.
3. Repositories persist domain state through JPA.
4. Flyway manages schema evolution.
5. Security filters and method guards enforce authentication and authorization.

Main modules:

| Module | Responsibility |
| --- | --- |
| `auth` | registration, login, refresh, logout, password reset, account activation |
| `security` | JWT parsing, authentication filter, CSRF handling, access checks |
| `organization` | organization CRUD and subscription entry points |
| `user` | user CRUD, pagination, status changes, imports, export |
| `content` | courses, chapters, lessons, resources |
| `classroom` | classrooms, members, classroom-course assignment |
| `enrollment` | enrollments, progress, certificates |
| `assessment` | tests, questions, attempts, results |
| `analytics` | class averages, failure rates, alerts, student stats |
| `feedback` | lesson ratings and question error reports |
| `subscription` / `payment` | subscription plans, checkout, Stripe webhook handling |
| `ai` | AI test generation, injection, adaptive sessions, curriculum catalog |
| `parent`, `student`, `role` | supporting identity and relationship models |

## Getting Started

### Prerequisites

Install the following locally:

- Java 17
- Maven 3.9+
- PostgreSQL 15+ recommended
- Docker and Docker Compose if you want containerized startup

Notes:

- On macOS/Linux, the repository currently expects a system `mvn` installation. There is no Unix `mvnw` wrapper script committed.
- On Windows, `mvnw.cmd` is available.

### Quick start

1. Create a `.env` file in the repository root.
2. Start PostgreSQL and create the main database.
3. Provide SMTP, Stripe, JWT, and AI settings.
4. Run `mvn spring-boot:run`.
5. Open `http://localhost:8067/swagger-ui/index.html`.

## Configuration

The application loads configuration from:

- `src/main/resources/application.yaml`
- optional root `.env` file through `spring.config.import=optional:file:.env`
- environment variables
- `src/test/resources/application.yaml` for tests

`application-dev.yml` and `application-prod.yml` are currently present but empty, so the base `application.yaml` carries the active non-test configuration.

### Required environment variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Yes | JDBC URL for the main PostgreSQL database |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | HMAC secret used to sign JWTs; use at least 32 characters/bytes |
| `MAIL_HOST` | Yes | SMTP host |
| `MAIL_PORT` | Yes | SMTP port |
| `MAIL_USERNAME` | Yes | SMTP username |
| `MAIL_PASSWORD` | Yes | SMTP password |
| `MAIL_FROM` | Yes | Sender address for activation and reset emails |
| `APP_FRONTEND_URL` | Yes | Frontend base URL used to build password reset and activation links |
| `AI_API_BASE_URL` | Yes | Base URL of the external AI service |
| `AI_API_KEY` | Yes | API key sent to the AI service as `X-API-Key` |
| `STRIPE_SECRET_KEY` | Yes | Stripe API secret key |
| `STRIPE_WEBHOOK_SECRET` | Yes | Stripe webhook signing secret |
| `PORT` | No | Application port; defaults to `8067` |
| `APP_AUTH_SECURE_COOKIES` | No | Defaults to `true`; set to `false` for local HTTP development |
| `APP_AUTH_API_PATH` | No | Refresh-cookie path; defaults to `/api/v1/auth` |

### Example `.env`

Use placeholders, not production secrets:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/adaptive_tutor
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password

JWT_SECRET=replace-this-with-a-secret-of-at-least-32-characters
PORT=8067

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=mailer@example.com
MAIL_PASSWORD=change-me
MAIL_FROM=no-reply@example.com
APP_FRONTEND_URL=http://localhost:3000

AI_API_BASE_URL=http://localhost:8081
AI_API_KEY=change-me

STRIPE_SECRET_KEY=sk_test_change_me
STRIPE_WEBHOOK_SECRET=whsec_change_me

APP_AUTH_SECURE_COOKIES=false
```

### Frontend and CORS notes

CORS is currently configured in code, not via externalized properties. The allowlist includes `http://localhost:3000` and a few specific Vercel deployments. If your frontend runs on a different origin, update `SecurityConfig` before expecting browser requests to succeed.

## Running the Application

### Run locally with Maven

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8067` unless `PORT` overrides it.

### Build a JAR

```bash
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

If you only want the artifact and do not want to execute the test suite during packaging:

```bash
mvn -DskipTests clean package
```

### Run with Docker Compose

The repository includes:

- `Dockerfile` for the backend image
- `docker-compose.yml` with:
  - PostgreSQL 15 (`adaptive_tutor`)
  - backend service on port `8067`
  - `init-test-db.sql` to create `adaptive_tutor_test`

Start everything with:

```bash
docker compose up --build
```

The compose setup reads `.env` and injects the main runtime secrets into the application container.

## API Documentation

OpenAPI and Swagger UI are enabled through `springdoc-openapi`.

- Swagger UI: `http://localhost:8067/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8067/v3/api-docs`
- Health endpoint: `http://localhost:8067/actuator/health`

Swagger is the authoritative source for the full request/response contract. Controllers are annotated extensively, especially in `auth`, `ai`, `assessment`, `organization`, `classroom`, and `subscription` areas.

### Publicly accessible routes

Based on the current security configuration, these route groups are public:

- `/api/v1/auth/**`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/swagger-ui.html`
- `/actuator/health`
- `/api/v1/payments/webhooks/stripe`
- `/errors`

All other routes require authentication.

## Authentication and Security

### Token model

Authentication is split between bearer tokens and refresh cookies:

- Access token: returned in JSON and sent by clients in `Authorization: Bearer <token>`
- Refresh token: sent as an `HttpOnly` cookie scoped to the auth API path

As implemented in `src/main/java/org/elearning/backend/security/jwt/JwtUtil.java`:

- access tokens expire after 30 minutes
- refresh tokens expire after 7 days
- access tokens contain the user UUID as `sub`
- access tokens also carry a `role` claim

### Refresh and logout flow

Refresh token rotation is handled by `POST /api/v1/auth/refresh`.

Logout behavior:

- revokes the refresh token if present
- attempts to blacklist the current access token until its expiration
- clears the refresh-token cookie

### CSRF behavior

CSRF is intentionally enforced only for endpoints that rely on cookie-authenticated state:

- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Expected browser flow:

1. Authenticate with `register` or `login`.
2. Fetch `GET /api/v1/auth/csrf` with credentials included.
3. Read the returned token and header name.
4. Send that token back in the `X-XSRF-TOKEN` header for `refresh` and `logout`.

```env
APP_AUTH_SECURE_COOKIES=false
```

### Authorization model

Access control is enforced with:

- Spring Security authentication filter for bearer JWTs
- `@PreAuthorize(...)` annotations on controllers
- `AccessService` for tenant-aware business authorization rules

Common patterns:

- `ADMIN` has platform-wide visibility
- `ORGANIZATION_ADMIN` is scoped to its organization
- `TEACHER`, `STUDENT`, and `PARENT` receive domain-specific access

## Database and Migrations

The application uses PostgreSQL with Flyway migrations located in:

```text
src/main/resources/db/migration
```

What happens on startup:

- Flyway is enabled
- schema validation is performed through Hibernate with `ddl-auto=validate`
- migrations create the platform schema for content, users, organizations, assessments, enrollment, tokens, classrooms, analytics, subscriptions, ratings, and AI jobs
- seed migrations also insert sample course data

Main databases used by the repository:

- `adaptive_tutor` for the application
- `adaptive_tutor_test` for tests

## Testing

Run the test suite with:

```bash
mvn test
```

Test coverage spans all major domains, including:

- auth and security
- content and assessments
- classrooms and organizations
- analytics and feedback
- subscriptions and Stripe webhook handling
- AI generation and adaptive learning flows

Important detail: although Testcontainers dependencies are present, the current test configuration points to a local PostgreSQL database at `jdbc:postgresql://localhost:5432/adaptive_tutor_test`. The included `docker-compose.yml` plus `init-test-db.sql` can create that database for you.

## Logging and Operations

Logging is configured through `src/main/resources/logback-spring.xml`.

Runtime log outputs:

- application log: `app.log`
- failed-login audit log: `logs/failed-login.log`
- archived rolling logs: `logs/archive/`

The security event listener records authentication failures to a dedicated file appender, which is useful for audit and incident review.

## Project Structure

```text
backend/
├── Dockerfile
├── docker-compose.yml
├── init-test-db.sql
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/org/elearning/backend/
│   │   │   ├── ai/
│   │   │   ├── analytics/
│   │   │   ├── assessment/
│   │   │   ├── auth/
│   │   │   ├── classroom/
│   │   │   ├── content/
│   │   │   ├── enrollment/
│   │   │   ├── feedback/
│   │   │   ├── organization/
│   │   │   ├── payment/
│   │   │   ├── security/
│   │   │   ├── subscription/
│   │   │   └── user/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── db/migration/
│   │       ├── fonts/
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/
│       └── resources/application.yaml
└── README.md
```

## Troubleshooting

### Browser login works but refresh/logout fails

Check all of the following:

- you called `GET /api/v1/auth/csrf`
- you send credentials/cookies with the request
- you echo the CSRF token in `X-XSRF-TOKEN`
- `APP_AUTH_SECURE_COOKIES=false` is set for local HTTP

### Frontend requests are blocked by CORS

Your frontend origin is likely not in the hard-coded allowlist in `SecurityConfig`.

### Tests fail on database connection

Create the `adaptive_tutor_test` database locally or start the provided compose stack.

### Email links point to the wrong host

Set `APP_FRONTEND_URL` to the correct frontend base URL. Activation and password-reset emails are built from that property.

### JWT startup or token-signing errors

Make sure `JWT_SECRET` is set and long enough for HMAC signing. A short secret can cause key initialization failures.
