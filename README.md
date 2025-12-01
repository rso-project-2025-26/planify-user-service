# User Service

This service manages users, organizations, and memberships in the Planify platform. It provides RESTful APIs secured with Keycloak authentication and publishes events via Kafka.

## System Integrations

-   **Keycloak**: OAuth2/OIDC authentication and authorization. All endpoints require a valid JWT Bearer token, except registration and organization creation.
-   **Kafka**: Publishes domain events for user/organization changes consumed by other services.
-   **PostgreSQL**: Persists all user, organization, and membership data via Hibernate/JPA with Flyway migrations in the `auth` schema.

## Roles

### Organization Roles

Roles within specific organizations, stored in the database:

-   **GUEST** — Can view organization events
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
-   `GET /api/auth/{orgId}/roles` — Get current user's role within organization

### Users (`/api/users`)

-   `GET /api/users` — List all users (ADMINISTRATOR only)
-   `GET /api/users/{orgId}/users` — List users in organization (ORG_ADMIN only)
-   `GET /api/users/me` — Get current authenticated user
-   `DELETE /api/users/me` — Delete current user account
-   `GET /api/users/me/export` — Export user data (GDPR compliance)
-   `POST /api/users/{orgId}/join-request` — Send join request to organization

### Organizations (`/api/organizations`)

-   `POST /api/organizations` — Create new organization
-   `GET /api/organizations/{orgId}/members` — List organization members (ORG_ADMIN only)
-   `POST /api/organizations/{orgId}/invite?userId={userId}&role={role}` — Invite user to organization (ORG_ADMIN only)
-   `DELETE /api/organizations/{orgId}/members/{userId}` — Remove user from organization (ORG_ADMIN only)
-   `POST /api/organizations/{orgId}/members/{userId}/role?newRole={role}` — Change user's role (ORG_ADMIN only)
-   `GET /api/organizations/{orgId}/join-requests` — List pending join requests (ORG_ADMIN only)
-   `POST /api/organizations/{orgId}/join-request/{requestId}/approve` — Approve join request (ORG_ADMIN only)
-   `POST /api/organizations/{orgId}/join-request/{requestId}/reject` — Reject join request (ORG_ADMIN only)

### Invitations (`/api/invitations`)

-   `GET /api/invitations` — List all invitations (ADMINISTRATOR only)
-   `POST /api/invitations/currentUser` — Get invitations for current user
-   `POST /api/invitations/{token}/accept` — Accept invitation
-   `POST /api/invitations/{token}/decline` — Decline invitation

## Database Structure

The service uses PostgreSQL with the following core entities in the `auth` schema:

### Users

Core user records linked to Keycloak via `keycloak_id`. Contains:

-   `id` (UUID, PK)
-   `keycloak_id` (unique, links to Keycloak)
-   `email`, `username`, `first_name`, `last_name`
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
-   `user_id`, `organization_id` (FKs, unique together)
-   `role` (GUEST, MEMBER, ORG_ADMIN)
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
-   `status` (PENDING, ACCEPTED, EXPIRED)
-   `expires_at`, `created_at`, `accepted_at`

**Relationships**: All entities use UUIDs and enforce referential integrity via foreign keys. Audit fields (`created_at`, etc.) track changes. Database schema is versioned via Flyway migrations in `src/main/resources/db/migration/`.
