# Configuration Management Guide

This document explains how configurations and secrets are managed in the user-service following cloud-native best practices.

## Table of Contents

-   [Overview](#overview)
-   [Configuration Files](#configuration-files)
-   [Environment Variables](#environment-variables)
-   [Local Development](#local-development)
-   [Docker Compose](#docker-compose)
-   [Kubernetes Deployment](#kubernetes-deployment)
-   [Security Best Practices](#security-best-practices)

## Overview

The user-service follows the [12-Factor App](https://12factor.net/) methodology, particularly:

-   **Config (III)**: Store config in the environment
-   **Dev/Prod Parity (X)**: Keep development, staging, and production as similar as possible

All configuration is externalized using environment variables with sensible defaults for local development.

## Configuration Files

### Application Configuration

-   **`application.yaml`**: Base configuration with environment variable placeholders
-   **`application-dev.yaml`**: Development profile (verbose logging, Swagger enabled)
-   **`application-prod.yaml`**: Production profile (minimal logging, Swagger disabled)
-   **`application-test.yaml`**: Test profile (H2 database, mocked external services)

### Environment Templates

-   **`.env.example`**: Complete documentation of all environment variables
-   **`.env.docker`**: Docker Compose environment template

### Kubernetes Manifests

-   **`kubernetes/user-service/configmap.yaml`**: Non-sensitive configuration
-   **`kubernetes/user-service/secrets.yaml`**: Sensitive credentials (base64 encoded)
-   **`kubernetes/user-service/deployment.yaml`**: Deployment with config/secret references

## Environment Variables

All environment variables are documented in `.env.example`. Key categories:

### Database

-   `DB_URL`: JDBC connection URL
-   `DB_USERNAME`: Database user
-   `DB_PASSWORD`: Database password (secret)
-   `DB_SCHEMA`: PostgreSQL schema name
-   `DB_POOL_SIZE`: Connection pool size

### Authentication

-   `KEYCLOAK_URL`: Keycloak server URL
-   `KEYCLOAK_REALM`: Realm name
-   `KEYCLOAK_ADMIN_USERNAME`: Admin user (secret)
-   `KEYCLOAK_ADMIN_PASSWORD`: Admin password (secret)

### Messaging

-   `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
-   `KAFKA_TOPIC_JOIN_REQUESTS`: Join request topic
-   `KAFKA_TOPIC_INVITATIONS`: Invitation topic

### Logging & Monitoring

-   `LOG_LEVEL_ROOT`: Root logger level
-   `LOG_LEVEL_APP`: Application logger level
-   `PROMETHEUS_ENABLED`: Enable Prometheus metrics

## Local Development

### Option 1: IDE Environment Variables

Configure environment variables in your IDE (IntelliJ, Eclipse, VS Code):

**IntelliJ IDEA:**

1. Run → Edit Configurations
2. Environment Variables → Add all from `.env.example`
3. Select profile: `-Dspring.profiles.active=dev`

**VS Code:**

```json
{
	"spring-boot.run.env": {
		"DB_URL": "jdbc:postgresql://localhost:5432/planify",
		"DB_USERNAME": "planify",
		"DB_PASSWORD": "planify",
		"SPRING_PROFILES_ACTIVE": "dev"
	}
}
```

### Option 2: System Environment Variables

```powershell
# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/planify"
$env:DB_USERNAME="planify"
$env:DB_PASSWORD="planify"
$env:SPRING_PROFILES_ACTIVE="dev"

mvn spring-boot:run
```

```bash
# Linux/macOS
export DB_URL=jdbc:postgresql://localhost:5432/planify
export DB_USERNAME=planify
export DB_PASSWORD=planify
export SPRING_PROFILES_ACTIVE=dev

mvn spring-boot:run
```

### Option 3: .env File (with spring-dotenv)

```powershell
# Copy template
cp .env.example .env

# Edit .env with your values
# Then add spring-dotenv dependency to pom.xml
```

## Docker Compose

1. **Copy the template:**

    ```powershell
    cp .env.docker .env
    ```

2. **Edit values** in `.env` for your local environment

3. **Run with compose:**
    ```powershell
    docker-compose --env-file .env up
    ```

The `docker-compose.yaml` will automatically load environment variables:

```yaml
services:
    user-service:
        env_file:
            - .env
        # or
        environment:
            - DB_URL=${DB_URL}
            - DB_USERNAME=${DB_USERNAME}
            - DB_PASSWORD=${DB_PASSWORD}
```

## Kubernetes Deployment

### Development/Staging

1. **Edit ConfigMap** with environment-specific values:

    ```yaml
    kubectl edit configmap user-service-config -n planify
    ```

2. **Create secrets** (DO NOT commit to git):

    ```powershell
    kubectl create secret generic user-service-secrets `
      --from-literal=DB_PASSWORD='your-db-password' `
      --from-literal=KEYCLOAK_ADMIN_PASSWORD='your-keycloak-password' `
      -n planify
    ```

3. **Deploy:**
    ```powershell
    kubectl apply -f kubernetes/user-service/
    ```

### Production

**Option 1: Sealed Secrets** (recommended)

```powershell
# Install sealed-secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.18.0/controller.yaml

# Seal your secrets
kubeseal --format yaml < secrets.yaml > sealed-secrets.yaml

# Commit sealed-secrets.yaml (encrypted, safe to commit)
kubectl apply -f sealed-secrets.yaml
```

**Option 2: External Secrets Operator**

```yaml
# Use Azure Key Vault, AWS Secrets Manager, etc.
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
    name: user-service-secrets
spec:
    secretStoreRef:
        name: azure-key-vault
    data:
        - secretKey: DB_PASSWORD
          remoteRef:
              key: planify-db-password
```

**Option 3: HashiCorp Vault**

```powershell
# Store secrets in Vault
vault kv put secret/planify/user-service `
  DB_PASSWORD="your-password" `
  KEYCLOAK_ADMIN_PASSWORD="your-password"

# Use Vault injector or CSI driver
```

## Security Best Practices

### ✅ DO:

-   Store all secrets in environment variables or secret management systems
-   Use different credentials for dev/staging/prod
-   Rotate secrets regularly
-   Use read-only secrets in containers when possible
-   Enable RBAC for Kubernetes secrets access
-   Use encrypted secret storage (Sealed Secrets, External Secrets)
-   Audit secret access with tools like Vault

### ❌ DON'T:

-   Commit secrets to Git (even in private repos)
-   Hard-code credentials in application code
-   Share production secrets via chat/email
-   Use default passwords in production
-   Store secrets in ConfigMaps (use Secrets)
-   Log sensitive configuration values

### .gitignore Protection

The following are excluded from version control:

```gitignore
.env
.env.local
.env.*.local
*.env
**/secrets.yaml.local
**/secrets-*.yaml
```

### Secret Scanning

Enable pre-commit hooks to prevent secret leaks:

```powershell
# Install git-secrets
git secrets --install
git secrets --register-aws
git secrets --add 'password\s*=\s*.+'
git secrets --add 'KEYCLOAK_ADMIN_PASSWORD.*'
```

## Configuration Validation

The application validates configuration at startup:

-   Database connectivity
-   Keycloak availability
-   Kafka broker connectivity
-   Required environment variables

Check startup logs for validation errors:

```
2024-01-15 10:00:00.123 ERROR - Configuration validation failed: DB_PASSWORD is not set
2024-01-15 10:00:00.456 ERROR - Cannot connect to Keycloak at http://keycloak:8080
```

## Troubleshooting

### Issue: Application won't start

**Check:** Environment variables are set correctly

```powershell
kubectl get configmap user-service-config -o yaml
kubectl get secret user-service-secrets -o yaml
```

### Issue: Database connection failed

**Check:** DB credentials and URL

```powershell
kubectl logs deployment/user-service -n planify
```

### Issue: Keycloak authentication fails

**Check:** Keycloak URL is accessible from pod

```powershell
kubectl exec -it deployment/user-service -n planify -- curl http://keycloak-service:8080
```

## References

-   [12-Factor App - Config](https://12factor.net/config)
-   [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
-   [Kubernetes ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)
-   [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
-   [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets)
-   [External Secrets Operator](https://external-secrets.io/)
