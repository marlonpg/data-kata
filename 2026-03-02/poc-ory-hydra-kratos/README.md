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
- [OAuth 2.0 Playground](https://www.oauth.com/playground/)
- [JWT Decoder](https://jwt.io/)


## Complete End-to-End Flow with cURL

### Scenario: Register User + OAuth Login Flow

This demonstrates the complete flow from user registration to getting OAuth tokens.

#### Prerequisites

1. Start all services: `docker-compose up -d`
2. Create an OAuth client first:

```bash
docker exec ory-hydra hydra create client \
  --endpoint http://localhost:4445 \
  --name "My Test App" \
  --grant-type authorization_code,refresh_token \
  --response-type code \
  --scope openid,offline,email \
  --redirect-uri http://localhost:3000/callback
```

Save the `client_id` and `client_secret` from output.

---

### Step 1: Register a New User (Kratos)

**1.1 Initialize registration flow:**
```bash
curl -X GET http://localhost:4433/self-service/registration/api
```

Save the `flow_id` from the response (e.g., `"id": "abc123..."`).

**1.2 Complete registration:**
```bash
curl -X POST http://localhost:4433/self-service/registration?flow=<FLOW_ID> \
  -H "Content-Type: application/json" \
  -d '{
    "method": "password",
    "traits": {
      "email": "user@example.com",
      "name": {
        "first": "John",
        "last": "Doe"
      }
    },
    "password": "SecurePassword123!"
  }'
```

User is now registered in Kratos! Save the `session_token` from response.

---

### Step 2: OAuth Login Flow (Hydra + Kratos)

**2.1 Start OAuth authorization (simulating browser redirect):**
```bash
curl -v "http://localhost:4444/oauth2/auth?client_id=<CLIENT_ID>&response_type=code&scope=openid%20offline%20email&redirect_uri=http://localhost:3000/callback&state=random_state_string"
```

This will return a redirect to the login page. Extract the `login_challenge` from the redirect URL.

**2.2 Get login challenge details:**
```bash
curl "http://localhost:4445/admin/oauth2/auth/requests/login?login_challenge=<LOGIN_CHALLENGE>"
```

**2.3 Initialize Kratos login flow:**
```bash
curl -X GET http://localhost:4433/self-service/login/api
```

Save the `flow_id`.

**2.4 Submit login credentials to Kratos:**
```bash
curl -X POST "http://localhost:4433/self-service/login?flow=<FLOW_ID>" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "password",
    "identifier": "user@example.com",
    "password": "SecurePassword123!"
  }'
```

Kratos validates credentials and returns session. Save the `session_token`.

**2.5 Accept login challenge (tell Hydra user is authenticated):**
```bash
curl -X PUT "http://localhost:4445/admin/oauth2/auth/requests/login/accept?login_challenge=<LOGIN_CHALLENGE>" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user@example.com",
    "remember": true,
    "remember_for": 3600
  }'
```

This returns a `redirect_to` URL with a `consent_challenge`.

**2.6 Get consent challenge details:**
```bash
curl "http://localhost:4445/admin/oauth2/auth/requests/consent?consent_challenge=<CONSENT_CHALLENGE>"
```

**2.7 Accept consent (user approves app access):**
```bash
curl -X PUT "http://localhost:4445/admin/oauth2/auth/requests/consent/accept?consent_challenge=<CONSENT_CHALLENGE>" \
  -H "Content-Type: application/json" \
  -d '{
    "grant_scope": ["openid", "offline", "email"],
    "grant_access_token_audience": [],
    "remember": true,
    "remember_for": 3600,
    "session": {
      "id_token": {
        "email": "user@example.com"
      }
    }
  }'
```

This returns a `redirect_to` URL with an authorization `code`.

**2.8 Exchange authorization code for tokens:**
```bash
curl -X POST http://localhost:4444/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "<CLIENT_ID>:<CLIENT_SECRET>" \
  -d "grant_type=authorization_code&code=<AUTHORIZATION_CODE>&redirect_uri=http://localhost:3000/callback"
```

**Success!** You now have:
- `access_token` - Use to access APIs
- `id_token` - Contains user identity info
- `refresh_token` - Use to get new access tokens

---

### Step 3: Validate and Use Tokens

**3.1 Introspect access token:**
```bash
curl -X POST http://localhost:4444/admin/oauth2/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=<ACCESS_TOKEN>"
```

**3.2 Get user info (OIDC):**
```bash
curl http://localhost:4444/userinfo \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**3.3 Refresh access token:**
```bash
curl -X POST http://localhost:4444/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "<CLIENT_ID>:<CLIENT_SECRET>" \
  -d "grant_type=refresh_token&refresh_token=<REFRESH_TOKEN>"
```

---

### Simplified Flow Summary

```
REGISTRATION (One-time):
1. Initialize registration flow → Get flow_id
2. Submit registration data to Kratos
3. User account created ✓

OAUTH LOGIN (Every time):
1. App redirects to Hydra /oauth2/auth → Get login_challenge
2. Initialize Kratos login flow → Get flow_id
3. Submit credentials to Kratos → Get session
4. Accept login challenge in Hydra → Get consent_challenge
5. Accept consent challenge → Get authorization code
6. Exchange code for tokens → Get access_token, id_token, refresh_token ✓
```

---

### Quick Test Script

For easier testing, you can use the Kratos UI at http://localhost:4455 (if added to docker-compose):

1. Visit http://localhost:4455/registration
2. Register a new user
3. Visit http://localhost:4455/login
4. Login with credentials
5. Check session: http://localhost:4455/sessions

This UI handles all the Kratos API calls for you!