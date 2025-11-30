# User Service

A Spring Boot 3 microservice for managing users, organizations, and memberships in the Planify platform. This service handles authentication via Keycloak, organization management, and user profiles.

## Overview

The User Service is a RESTful API built with Spring Boot 3.5.7 that provides:

-   **User Management** - User profiles, creation, and metadata
-   **Organization Management** - Create, read, and manage organizations
-   **Organization Memberships** - Manage user roles within organizations
-   **Join Requests** - Allow users to request access to organizations
-   **Invitations** - Invite users to organizations via email tokens
-   **OAuth2/OIDC Authentication** - Secure endpoints with Keycloak JWT validation
-   **Database Persistence** - PostgreSQL with Hibernate/JPA ORM
-   **Database Migrations** - Flyway for schema versioning
-   **Event Publishing** - Kafka integration for event-driven architecture

## Technology Stack

| Component           | Version | Purpose                        |
| ------------------- | ------- | ------------------------------ |
| **Java**            | 21      | Programming language           |
| **Spring Boot**     | 3.5.7   | Application framework          |
| **Spring Security** | 6.2.x   | Authentication & authorization |
| **Spring Data JPA** | 3.2.x   | ORM & database access          |
| **Hibernate**       | 6.6.33  | JPA implementation             |
| **PostgreSQL**      | 16      | Relational database            |
| **Flyway**          | 9.x     | Database migrations            |
| **Kafka**           | Latest  | Event streaming                |
| **Keycloak**        | 24.0.1  | OAuth2/OIDC identity provider  |
| **Maven**           | 3.x     | Build tool                     |
| **Docker**          | Latest  | Containerization               |

## Architecture

### Project Structure

```
src/
├── main/
│   ├── java/com/planify/user_service/
│   │   ├── UserServiceApplication.java        # Application entry point
│   │   ├── config/
│   │   │   ├── SecurityConfig.java            # Spring Security configuration
│   │   │   └── KeycloakJwtConverter.java      # JWT to authorities converter
│   │   ├── controller/
│   │   │   ├── OrganizationController.java    # Organization endpoints
│   │   │   ├── GuestListController.java       # Guest list endpoints
│   │   │   └── UserController.java            # User endpoints
│   │   ├── service/
│   │   │   ├── OrganizationService.java       # Organization business logic
│   │   │   ├── GuestListService.java          # Guest list business logic
│   │   │   └── UserService.java               # User business logic
│   │   ├── repository/
│   │   │   ├── UserRepository.java            # User data access
│   │   │   ├── OrganizationRepository.java    # Organization data access
│   │   │   ├── OrganizationMembershipRepository.java
│   │   │   ├── JoinRequestRepository.java     # Join request data access
│   │   │   └── InvitationRepository.java      # Invitation data access
│   │   ├── model/
│   │   │   ├── UserEntity.java                # User JPA entity
│   │   │   ├── OrganizationEntity.java        # Organization JPA entity
│   │   │   ├── OrganizationMembershipEntity.java
│   │   │   ├── JoinRequestEntity.java         # Join request JPA entity
│   │   │   ├── InvitationEntity.java          # Invitation JPA entity
│   │   │   ├── OrganizationType.java          # Enum (PERSONAL, TEAM, BUSINESS)
│   │   │   ├── OrganizationRole.java          # Enum (GUEST, MEMBER, ORG_ADMIN)
│   │   │   ├── JoinRequestStatus.java         # Enum (PENDING, ACCEPTED, REJECTED)
│   │   │   └── InvitationStatus.java          # Enum (PENDING, ACCEPTED, EXPIRED)
│   │   └── event/
│   │       ├── KafkaProducer.java             # Kafka event publisher
│   │       └── KafkaConsumer.java             # Kafka event listener
│   └── resources/
│       ├── application.yaml                   # Application configuration
│       └── db/migration/
│           └── V1__init.sql                   # Initial database schema
└── test/
    └── java/com/planify/user_service/
        └── EventManagerApplicationTests.java  # Unit tests
```

### Entity Relationship Diagram

```
┌─────────┐          ┌──────────────────────┐
│  Users  │          │ Organizations        │
├─────────┤          ├──────────────────────┤
│ id (UUID)├─────────>│ id (UUID)            │
│ email   │          │ name                 │
│ display │  1:N     │ slug                 │
│ name    │          │ created_by_user_id ──┐
└─────────┘          └──────────────────────┘
     │                          │
     │ 1:N                  1:N │
     ↓                          ↓
┌──────────────────────────┐  ┌─────────────┐
│ OrganizationMemberships  │  │ JoinRequests│
├──────────────────────────┤  ├─────────────┤
│ id (UUID)                │  │ id (UUID)   │
│ user_id (FK)             │  │ user_id (FK)│
│ organization_id (FK)     │  │ org_id (FK) │
│ role                     │  │ status      │
└──────────────────────────┘  └─────────────┘

     ┌─────────────┐
     │ Invitations │
     ├─────────────┤
     │ id (UUID)   │
     │ org_id (FK) │
     │ email       │
     │ status      │
     │ token       │
     └─────────────┘
```

## Prerequisites

### Required

-   **Java 21** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/)
-   **Maven 3.8+** - [Download from Apache](https://maven.apache.org/download.cgi)
-   **Docker & Docker Compose** - [Download Docker Desktop](https://www.docker.com/products/docker-desktop)
-   **PostgreSQL 16** (via Docker) - Included in `docker-compose.yaml`
-   **Keycloak 24.0.1** (via Docker) - Included in `docker-compose.yaml`
-   **Kafka** (via Docker) - Optional for event streaming

### Optional

-   **IDE** - IntelliJ IDEA, Eclipse, or VS Code with Spring Boot extensions
-   **Postman or REST Client** - For API testing
-   **DBeaver** - For database exploration

## Quick Start

### 1. Start Infrastructure (PostgreSQL, Keycloak)

```bash
# From project root
cd infrastructure
docker-compose up -d
```

This starts:

-   **PostgreSQL** on `localhost:5432`
-   **Keycloak** on `localhost:9080`
-   **Kafka** on `localhost:9092`

### 2. Build the Service

```bash
cd services/user-service

# Build without running tests
./mvnw clean package -DskipTests

# Or build and run tests (requires DB running)
./mvnw clean package
```

### 3. Run the Application

```bash
# Option 1: Using Maven
./mvnw spring-boot:run

# Option 2: Using Java directly
java -jar target/user-service-1.0.0-SNAPSHOT.jar

# Option 3: Using Docker
docker build -t planify/user-service:1.0.0 .
docker run -p 8082:8082 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify \
  -e SPRING_DATASOURCE_USERNAME=planify \
  -e SPRING_DATASOURCE_PASSWORD=planify \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:9080/realms/planify \
  planify/user-service:1.0.0
```

### 4. Verify Service is Running

```bash
# Health check
curl http://localhost:8082/actuator/health

# Expected response:
# {"status":"UP"}
```

## Configuration

### application.yaml

Key configuration settings:

```yaml
server:
    port: 8082
    servlet:
        context-path: /

spring:
    application:
        name: user-service

    datasource:
        url: jdbc:postgresql://localhost:5432/planify
        username: planify
        password: planify
        driver-class-name: org.postgresql.Driver

    jpa:
        database-platform: org.hibernate.dialect.PostgreSQLDialect
        hibernate:
            ddl-auto: validate # Production: validate, Testing: create-drop
        show-sql: false

    flyway:
        enabled: true
        locations: classpath:db/migration

    security:
        oauth2:
            resourceserver:
                jwt:
                    issuer-uri: http://localhost:9080/realms/planify
                    jwk-set-uri: http://localhost:9080/realms/planify/protocol/openid-connect/certs

    kafka:
        bootstrap-servers: localhost:9092
        producer:
            value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
        consumer:
            value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
            group-id: user-service
```

### Environment Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/planify
SPRING_DATASOURCE_USERNAME=planify
SPRING_DATASOURCE_PASSWORD=planify

# Keycloak OAuth2
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:9080/realms/planify
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://localhost:9080/realms/planify/protocol/openid-connect/certs

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Hibernate
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_SHOW_SQL=false
```

## API Endpoints

### Authentication Required (Bearer Token)

All endpoints require a valid JWT token from Keycloak in the `Authorization` header:

```
Authorization: Bearer <JWT_TOKEN>
```

### Organizations

| Method | Endpoint                              | Description               | Returns                 |
| ------ | ------------------------------------- | ------------------------- | ----------------------- |
| POST   | `/api/organizations`                  | Create organization       | 201 Created             |
| GET    | `/api/organizations/my-organizations` | List user's organizations | 200 OK - Organization[] |
| GET    | `/api/organizations/{id}`             | Get organization details  | 200 OK - Organization   |
| PUT    | `/api/organizations/{id}`             | Update organization       | 200 OK - Organization   |
| DELETE | `/api/organizations/{id}`             | Delete organization       | 204 No Content          |

#### Create Organization

```bash
curl -X POST http://localhost:8082/api/organizations \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Team",
    "slug": "my-team",
    "description": "Team description",
    "type": "TEAM"
  }'
```

### Users

| Method | Endpoint               | Description                    |
| ------ | ---------------------- | ------------------------------ |
| GET    | `/api/users/me`        | Get current authenticated user |
| GET    | `/api/users/{id}`      | Get user by ID                 |
| DELETE | `/api/users/me`        | Delete current user account    |
| GET    | `/api/users/me/export` | Export user data (GDPR)        |

#### Get Current User

```bash
curl -X GET http://localhost:8082/api/users/me \
  -H "Authorization: Bearer <TOKEN>"
```

### Organization Memberships

| Method | Endpoint                                   | Description                |
| ------ | ------------------------------------------ | -------------------------- |
| GET    | `/api/organizations/{id}/members`          | List organization members  |
| POST   | `/api/organizations/{id}/members`          | Add member to organization |
| DELETE | `/api/organizations/{id}/members/{userId}` | Remove member              |

### Join Requests

| Method | Endpoint                                | Description                    |
| ------ | --------------------------------------- | ------------------------------ |
| POST   | `/api/organizations/{id}/join-requests` | Request access to organization |
| GET    | `/api/organizations/{id}/join-requests` | List pending join requests     |
| POST   | `/api/join-requests/{id}/approve`       | Approve join request           |
| POST   | `/api/join-requests/{id}/reject`        | Reject join request            |

### Invitations

| Method | Endpoint                              | Description       |
| ------ | ------------------------------------- | ----------------- |
| POST   | `/api/organizations/{id}/invitations` | Send invitation   |
| GET    | `/api/organizations/{id}/invitations` | List invitations  |
| POST   | `/api/invitations/{token}/accept`     | Accept invitation |
| DELETE | `/api/invitations/{token}`            | Revoke invitation |

## Testing

### Unit Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=OrganizationServiceTest

# Run with coverage
./mvnw clean test jacoco:report
```

### Integration Tests

Tests require:

-   PostgreSQL running
-   Keycloak running
-   Test configuration in `src/test/resources/application.yaml`

```bash
./mvnw test -Dgroups=integration
```

### Manual API Testing

Use the included test scripts or Postman:

```bash
# Get Keycloak token
export TOKEN=$(curl -s -X POST http://localhost:9080/realms/planify/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=user-service&client_secret=S783QdEWAq21qdgWYWhhRslTDEXck2uu&grant_type=client_credentials" \
  | jq -r '.access_token')

# Create organization
curl -X POST http://localhost:8082/api/organizations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Org","description":"A test"}'

# List organizations
curl -X GET http://localhost:8082/api/organizations/my-organizations \
  -H "Authorization: Bearer $TOKEN"
```

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);
```

### Organizations Table

```sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(50) NOT NULL DEFAULT 'PERSONAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id UUID NOT NULL REFERENCES users(id)
);
```

### Organization Memberships Table

```sql
CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    role VARCHAR(50) NOT NULL DEFAULT 'GUEST',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(user_id, organization_id)
);
```

## Authentication Flow

### OAuth2 Authorization Code Flow

1. **User initiates login** → Frontend redirects to Keycloak
2. **User authenticates** with Keycloak (username/password)
3. **Keycloak redirects** back with authorization code
4. **Frontend exchanges code** for access token via Keycloak
5. **Frontend stores token** in localStorage
6. **Frontend includes token** in all API requests

### JWT Validation

-   Service validates JWT signature using Keycloak's public key
-   Extracts user ID (subject) from JWT claims
-   Validates token expiration and scopes
-   Maps user ID to database user for authorization

## Deployment

### Docker

Build and push to registry:

```bash
# Build image
docker build -t planify/user-service:1.0.0 .

# Tag for registry
docker tag planify/user-service:1.0.0 myregistry.azurecr.io/planify/user-service:1.0.0

# Push to registry
docker push myregistry.azurecr.io/planify/user-service:1.0.0
```

### Kubernetes

Helm chart or Kubernetes manifests coming soon. For now, use docker-compose or Docker Swarm.

### Environment Setup Checklist

-   [ ] PostgreSQL running and accessible
-   [ ] Keycloak running with realm and client configured
-   [ ] Database migrations applied
-   [ ] Kafka running (for event streaming)
-   [ ] Environment variables set correctly
-   [ ] JWT issuer URI points to correct Keycloak instance
-   [ ] CORS enabled for frontend origin
-   [ ] Logging configured to appropriate level

## Troubleshooting

### Common Issues

#### 1. Connection Refused (PostgreSQL)

```
Error: java.net.ConnectException: Connection refused
```

**Solution:**

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# If not, start docker-compose
cd infrastructure && docker-compose up -d
```

#### 2. Schema Validation Error

```
Error: Schema-validation: missing table [organizations]
```

**Solution:**

-   Ensure Flyway migrations have run: `ddl-auto: validate` requires existing tables
-   Check Flyway migration directory: `src/main/resources/db/migration/`
-   Manually run migration: `docker exec planify-postgres psql -U planify -d planify -f /path/to/V1__init.sql`

#### 3. Keycloak Token Invalid

```
Error: Invalid JWT signature
```

**Solution:**

-   Verify Keycloak realm name is correct: `http://localhost:9080/realms/planify`
-   Check JWT issuer URI matches: `http://localhost:9080/realms/planify`
-   Verify JWK set URI is accessible: `http://localhost:9080/realms/planify/protocol/openid-connect/certs`

#### 4. CORS Errors

```
Error: Access-Control-Allow-Origin missing
```

**Solution:**

-   Add CORS configuration in `SecurityConfig.java`
-   Enable CORS for your frontend origin (e.g., `http://localhost:4200`)

#### 5. User Not Found

```
Error: User not found: <uuid>
```

**Solution:**

-   Ensure user exists in database: `SELECT * FROM users WHERE id = '<uuid>';`
-   Check Keycloak user ID matches database ID
-   If mismatch, create user in database: `INSERT INTO users (id, keycloak_id, email, display_name) VALUES (...)`

## Performance Optimization

### Database Indexes

Already created in migration for:

-   `users.keycloak_id`
-   `organizations.created_by_user_id`
-   `organization_memberships.user_id`
-   `organization_memberships.organization_id`
-   `join_requests.organization_id`
-   `invitations.token`

### Caching

Consider adding:

-   Spring Cache with Redis
-   Keycloak token caching
-   Organization lookup caching

### Connection Pooling

HikariCP is configured with:

-   Maximum pool size: 10
-   Minimum idle: 5
-   Connection timeout: 30 seconds

## Contributing

When adding new features:

1. **Create entity** in `model/`
2. **Create repository** in `repository/`
3. **Create service** in `service/`
4. **Create controller** in `controller/`
5. **Add database migration** in `db/migration/` (V2\_\_feature.sql, etc.)
6. **Write tests** in `test/`
7. **Update documentation** in README.md

## License

MIT License - See LICENSE file in project root

## Support

For issues or questions:

-   Check Troubleshooting section above
-   Review Spring Boot documentation: https://spring.io/projects/spring-boot
-   Check Keycloak documentation: https://www.keycloak.org/documentation
-   Review source code comments and javadocs

---

**Last Updated:** November 28, 2025  
**Maintained By:** Planify Development Team
