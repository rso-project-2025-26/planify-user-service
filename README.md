# User Service

Microservice for managing users, organizations, and members in the Planify platform. Provides RESTful APIs secured with Keycloak authentication and publishes events via Kafka.

## Technologies

### Backend Framework & Language
- **Java 21** - Programming language
- **Spring Boot 3.5.7** - Application framework
- **Spring Security** - Security and authentication
- **Spring Data JPA** - Database access
- **Hibernate** - ORM framework
- **Lombok** - Boilerplate code reduction

### Database
- **PostgreSQL** - Database
- **Flyway** - Database migrations
- **HikariCP** - Connection pooling

### Security & Authentication
- **Keycloak** - OAuth2/OIDC authentication and authorization
- **Spring OAuth2 Resource Server** - JWT validation

### Messaging System
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration

### Monitoring & Health
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer Prometheus** - Metrics export
- **Resilience4j** - Circuit breakers, retry, rate limiting, bulkheads

### API Documentation
- **SpringDoc OpenAPI 3** - OpenAPI/Swagger documentation

### Containerization
- **Docker** - Application containerization
- **Kubernetes/Helm** - Orchestration (Helm charts included)

## System Integrations

-   **Keycloak**: OAuth2/OIDC authentication and authorization. All endpoints require a valid JWT Bearer token, except registration and organization creation.
-   **Kafka**: Publishes domain events for user/organization changes consumed by other services.
-   **PostgreSQL**: Stores all user, organization, and membership data via Hibernate/JPA with Flyway migrations in the `auth` schema.

## Roles

### Organization Roles

Roles within specific organizations, stored in the database:

-   **GUEST** — Can view organization events and event to which the user is invited
-   **ORGANISER** — Can create and edit events within the organization
-   **ORG_ADMIN** — Full organization admin permissions + organiser capabilities

### Keycloak Roles

Application-wide roles managed by Keycloak:

-   **UPORABNIK** — Standard authenticated user
-   **ADMINISTRATOR** — System administrator with access to all users and organizations

## API Endpoints

All endpoints require `Authorization: Bearer <JWT_TOKEN>` header unless otherwise specified.

### Authentication (`/api/auth`)

-   `POST /api/auth/register` — Register new user in system and Keycloak
-   `GET /api/auth/{orgId}/roles` — Get current user's roles within organization

### Users (`/api/users`)

-   `GET /api/users` — List all users (ADMINISTRATOR only)
-   `GET /api/users/{userId}` — Get user data by ID
-   `GET /api/users/search?username={query}` — Search users by username (ORG_ADMIN only)
-   `GET /api/users/me` — Get current authenticated user
-   `GET /api/users/me/orgs` — List organizations the user is a member of
-   `GET /api/users/me/join-requests` — List sent join requests to organizations
-   `GET /api/users/me/export` — Export user data (GDPR compliance)
-   `DELETE /api/users/me` — Delete current user account
-   `POST /api/users/{orgId}/join-request` — Send join request to organization

### Organizations (`/api/organizations`)

-   `POST /api/organizations` — Create new organization
-   `GET /api/organizations/admin/org` — Get organization ID of currently logged in ORG_ADMIN
-   `GET /api/organizations/search?query={slug}` — Search organizations by slug
-   `GET /api/organizations/{orgId}/members` — List organization members (ORG_ADMIN only)
-   `GET /api/organizations/{orgId}/keycloak/members` — List Keycloak IDs of organization members (ORG_ADMIN only)
-   `GET /api/organizations/{orgId}/join-requests` — List pending join requests (ORG_ADMIN only)
-   `POST /api/organizations/{orgId}/invite?userId={userId}&role={role}` — Invite user to organization (ORG_ADMIN only)
-   `POST /api/organizations/{orgId}/join-request/{requestId}/approve` — Approve join request (ORG_ADMIN only)
-   `POST /api/organizations/{orgId}/join-request/{requestId}/reject` — Reject join request (ORG_ADMIN only)
-   `PUT /api/organizations/{orgId}/members/{userId}/role?newRoles={role1,role2}` — Change user's roles (ORG_ADMIN only)
-   `DELETE /api/organizations/{orgId}/members/{userId}` — Remove user from organization (ORG_ADMIN only)
-   `DELETE /api/organizations/me/memberships/{orgId}` — Current user leaves organization

### Invitations (`/api/invitations`)

-   `GET /api/invitations` — List all invitations (ADMINISTRATOR only)
-   `GET /api/invitations/{orgId}/pending` — List pending invitations for organization (ORG_ADMIN only)
-   `GET /api/invitations/currentUser` — Get invitations for current user
-   `POST /api/invitations/{token}/accept` — Accept invitation
-   `POST /api/invitations/{token}/decline` — Decline invitation

## Database Structure

The service uses PostgreSQL with the following core entities in the `auth` schema:

### Users

Core user records linked to Keycloak via `keycloak_id`. Contains:

-   `id` (UUID, PK)
-   `keycloak_id` (UUID, unique, link to Keycloak)
-   `email`, `username`, `first_name`, `last_name`, `phone_number`
-   `consent` (boolean, GDPR consent)
-   `created_at`, `deleted_at` (soft delete support)

### Organizations

Groups managed by users. Contains:

-   `id` (UUID, PK)
-   `name`, `slug` (unique identifier)
-   `description`, `type` (PERSONAL, TEAM, BUSINESS)
-   `created_by_user_id` (FK to users)
-   `created_at`

### Organization Memberships

Links users to organizations with roles. Contains:

-   `id` (UUID, PK)
-   `user_id`, `organization_id` (FKs)
-   `role` (GUEST, ORGANISER, ORG_ADMIN)
-   `created_at`, `updated_at`

### Join Requests

Tracks user requests to join organizations. Contains:

-   `id` (UUID, PK)
-   `user_id`, `organization_id` (FKs)
-   `status` (PENDING, ACCEPTED, REJECTED)
-   `handled_by_user_id` (FK to approver/rejecter)
-   `created_at`, `handled_at`

### Invitations

Token-based invitations to join organizations. Contains:

-   `id` (UUID, PK)
-   `organization_id`, `user_id`, `created_by_user_id` (FKs)
-   `token` (unique), `role`
-   `status` (PENDING, ACCEPTED, DECLINED, EXPIRED)
-   `expires_at`, `created_at`, `accepted_at`

**Relationships**: All entities use UUIDs and enforce referential integrity via foreign keys. Audit fields (`created_at`, etc.) track changes. Database schema is versioned via Flyway migrations in `src/main/resources/db/migration/`.

## Installation and Setup

### Prerequisites

-   Java 21 or newer
-   Maven 3.6+
-   Docker and Docker Compose
-   Git

### Infrastructure Setup

This service requires PostgreSQL, Kafka, and Keycloak to run. These dependencies are provided via Docker containers in the main Planify repository.

**Clone and setup the infrastructure:**

```bash
# Clone the main Planify repository
git clone https://github.com/rso-project-2025-26/planify.git
cd planify

# Follow the setup instructions in the main repository README
# This will start all required infrastructure services (PostgreSQL, Kafka, Keycloak)
```

Refer to the main Planify repository (https://github.com/rso-project-2025-26/planify) documentation for detailed infrastructure setup instructions.

### Configuration

The application uses a single `application.yaml` configuration file located in `src/main/resources/`.

Important environment variables:
```bash
SERVER_PORT=8082
DB_URL=jdbc:postgresql://localhost:5432/planify
DB_USERNAME=planify
DB_PASSWORD=planify
DB_SCHEMA=auth
KEYCLOAK_ISSUER_URI=http://localhost:9080/realms/planify
KEYCLOAK_JWK_SET_URI=http://localhost:9080/realms/planify/protocol/openid-connect/certs
KEYCLOAK_URL=http://localhost:9080
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Local Run

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run
```

### Using Makefile

```bash
# Build project
make build

# Docker build
make docker-build

# Docker run
make docker-run

# Tests
make test
```

### Docker Run

```bash
# Build Docker image
docker build -t planify/user-service:0.0.1 .

# Run container
docker run -p 8082:8082 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/planify \
  -e KEYCLOAK_ISSUER_URI=http://host.docker.internal:9080/realms/planify \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  planify/user-service:0.0.1
```

### Kubernetes/Helm Deployment

```bash
# Install with Helm
helm install user-service ./helm/user

# Upgrade
helm upgrade user-service ./helm/user

# Uninstall
helm uninstall user-service
```

## Flyway Migrations

Migrations are located in `src/main/resources/db/migration/`:
- `V1__init.sql` - Table initialization
- `V2__remove_org_membership_unique_constraint.sql` - Remove unique constraint
- `V3__keycloak_id_to_uuid.sql` - Change keycloak_id to UUID
- `V4__consent_added.sql` - Added field for GDPR consent
- `V5__phone_number_added.sql` - Added field for phone number

Manual migration run:
```bash
mvn flyway:migrate
```

## Health Check & Monitoring

### Actuator Endpoints

-   `GET /actuator/health` — Health check endpoint
-   `GET /actuator/prometheus` — Prometheus metrics
-   `GET /actuator/info` — Application information

### API Documentation

After starting the application, Swagger UI is available at:
```
http://localhost:8082/swagger-ui.html
```

OpenAPI specification:
```
http://localhost:8082/v3/api-docs
```

## Kafka Events

The service publishes the following events to Kafka:

### Join Request Events
-   **join-request-sent** — User sends a join request to an organization
    - Contains: joinRequestId, organizationId, organizationName, requesterUserId, requesterUsername, adminIds, timestamp
-   **join-request-responded** — Organization admin responds to a join request (APPROVED/REJECTED)
    - Contains: joinRequestId, eventType, organizationId, organizationName, requesterUserId, requesterFirstName, requesterLastName, requesterEmail, timestamp

### Invitation Events
-   **invitation-sent** — Organization admin invites a user to join
    - Contains: invitationId, organizationId, organizationName, invitedUserId, invitedFirstName, invitedLastName, invitedEmail, timestamp
-   **invitation-responded** — User responds to an invitation (ACCEPTED/DECLINED)
    - Contains: invitationId, eventType, organizationId, organizationName, invitedUserId, invitedUsername, adminIds, timestamp

## Resilience4j

The service implements:
- **Circuit Breakers** - Prevention of cascading failures
- **Retry** - Automatic retry of failed calls
- **Rate Limiting** - Request rate limiting
- **Bulkheads** - Resource isolation

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=UserServiceTest

# Run with coverage report
mvn test jacoco:report
```
