# Ory Hydra + Kratos POC

This POC demonstrates running Ory Hydra (OAuth/OIDC) and Ory Kratos (Identity Management) locally with Docker.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                                                          │
│  ┌──────────────┐         ┌──────────────┐             │
│  │ Ory Hydra    │         │ Ory Kratos   │             │
│  │ (OAuth)      │         │ (Identity)   │             │
│  │              │         │              │             │
│  │ :4444 Public │         │ :4433 Public │             │
│  │ :4445 Admin  │         │ :4434 Admin  │             │
│  └──────┬───────┘         └──────┬───────┘             │
│         │                        │                      │
│         v                        v                      │
│  ┌──────────────┐         ┌──────────────┐             │
│  │ PostgreSQL   │         │ PostgreSQL   │             │
│  │ (Hydra DB)   │         │ (Kratos DB)  │             │
│  │ :5432        │         │ :5433        │             │
│  └──────────────┘         └──────────────┘             │
│                                                          │
│  ┌──────────────────────────────────────┐              │
│  │ MailSlurper (Email Testing)          │              │
│  │ :4436 Web UI                         │              │
│  │ :4437 SMTP                           │              │
│  └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

## Services

### Ory Hydra
- **Public API**: http://localhost:4444
- **Admin API**: http://localhost:4445
- **Purpose**: OAuth 2.0 and OpenID Connect server
- **Database**: PostgreSQL on port 5432

### Ory Kratos
- **Public API**: http://localhost:4433
- **Admin API**: http://localhost:4434
- **Purpose**: Identity and user management
- **Database**: PostgreSQL on port 5433

### MailSlurper
- **Web UI**: http://localhost:4436
- **Purpose**: Catch and view emails sent by Kratos (verification, recovery, etc.)

## Prerequisites

- Docker Desktop installed and running
- Docker Compose installed (included with Docker Desktop)

## Quick Start

### 1. Start all services

```bash
cd poc-ory-hydra-kratos
docker-compose up -d
```

### 2. Check services are running

```bash
docker-compose ps
```

You should see all services running:
- ory-hydra
- ory-kratos
- ory-hydra-db
- ory-kratos-db
- ory-mailslurper

### 3. View logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f hydra
docker-compose logs -f kratos
```

### 4. Stop services

```bash
docker-compose down
```

### 5. Stop and remove all data

```bash
docker-compose down -v
```

## Testing the Setup

### Test Hydra (OAuth Server)

**Check Hydra is running:**
```bash
curl http://localhost:4444/.well-known/openid-configuration
```

**Create an OAuth client:**
```bash
docker exec ory-hydra hydra create client \
  --endpoint http://localhost:4445 \
  --name "Test Client" \
  --grant-type authorization_code,refresh_token \
  --response-type code \
  --scope openid,offline \
  --redirect-uri http://localhost:3000/callback
```

Save the `client_id` and `client_secret` from the output.

**List OAuth clients:**
```bash
docker exec ory-hydra hydra list clients --endpoint http://localhost:4445
```

### Test Kratos (Identity Management)

**Check Kratos is running:**
```bash
curl http://localhost:4433/health/ready
```

**Create a test user (via Admin API):**
```bash
curl -X POST http://localhost:4434/admin/identities \
  -H "Content-Type: application/json" \
  -d '{
    "schema_id": "default",
    "traits": {
      "email": "test@example.com",
      "name": {
        "first": "John",
        "last": "Doe"
      }
    }
  }'
```

**List identities:**
```bash
curl http://localhost:4434/admin/identities
```

**Initialize registration flow:**
```bash
curl http://localhost:4433/self-service/registration/browser
```

### Test Email Delivery

1. Open MailSlurper: http://localhost:4436
2. Trigger an email from Kratos (e.g., registration with verification)
3. Check MailSlurper UI to see the email

## API Endpoints

### Hydra Public API (Port 4444)
- `GET /.well-known/openid-configuration` - OIDC discovery
- `GET /.well-known/jwks.json` - Public keys
- `GET /oauth2/auth` - Authorization endpoint
- `POST /oauth2/token` - Token endpoint
- `GET /userinfo` - UserInfo endpoint

### Hydra Admin API (Port 4445)
- `GET /admin/clients` - List OAuth clients
- `POST /admin/clients` - Create OAuth client
- `GET /admin/oauth2/auth/requests/login` - Get login request
- `PUT /admin/oauth2/auth/requests/login/accept` - Accept login
- `GET /admin/oauth2/auth/requests/consent` - Get consent request
- `PUT /admin/oauth2/auth/requests/consent/accept` - Accept consent

### Kratos Public API (Port 4433)
- `GET /self-service/registration/browser` - Start registration
- `GET /self-service/login/browser` - Start login
- `GET /self-service/recovery/browser` - Start recovery
- `GET /self-service/verification/browser` - Start verification
- `GET /sessions/whoami` - Get current session

### Kratos Admin API (Port 4434)
- `GET /admin/identities` - List identities
- `POST /admin/identities` - Create identity
- `GET /admin/identities/{id}` - Get identity
- `DELETE /admin/identities/{id}` - Delete identity

## Configuration Files

### docker-compose.yml
Main orchestration file defining all services and their configuration.

### kratos/kratos.yml
Kratos configuration:
- Database connection
- Self-service flow URLs
- Authentication methods (password, TOTP, etc.)
- Email settings

### kratos/identity.schema.json
Defines user attributes:
- Email (required, used for login)
- First name
- Last name

## Next Steps

To build a complete authentication system, you need to:

1. **Build Login/Consent UI** (Port 3000)
   - Login page for Kratos
   - Consent page for Hydra
   - Registration page
   - Account settings page

2. **Integrate Hydra + Kratos**
   - Hydra redirects to your login page
   - Your login page uses Kratos to authenticate
   - After authentication, accept Hydra's login challenge
   - Show consent screen and accept consent challenge

3. **Build Your Application**
   - Integrate OAuth client
   - Validate tokens from Hydra
   - Use Kratos sessions for user management

## Troubleshooting

**Services won't start:**
```bash
# Check logs
docker-compose logs

# Restart services
docker-compose restart
```

**Port conflicts:**
If ports are already in use, edit `docker-compose.yml` and change the port mappings.

**Database issues:**
```bash
# Reset databases
docker-compose down -v
docker-compose up -d
```

**Can't connect to services:**
Make sure Docker Desktop is running and services are healthy:
```bash
docker-compose ps
```

## Resources

- [Ory Hydra Docs](https://www.ory.sh/hydra/docs/)
- [Ory Kratos Docs](https://www.ory.sh/kratos/docs/)
- [Ory Community](https://community.ory.sh/)
- [Example Integration](https://github.com/ory/hydra-login-consent-node)
