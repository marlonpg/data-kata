# Zitadel POC

This POC demonstrates running Zitadel locally with Docker - a complete identity and access management platform.

## What is Zitadel?

Zitadel is a modern, cloud-native IAM platform that combines:
- **User Management** (like Kratos)
- **OAuth/OIDC Server** (like Hydra)
- **Admin UI** (built-in)
- **Multi-tenancy** (organizations and projects)
- **Event Sourcing Architecture**

Unlike Hydra + Kratos (2 separate services), Zitadel is an all-in-one solution.

## Architecture

```
┌─────────────────────────────────────────┐
│         Docker Network                   │
│                                          │
│  ┌────────────────────────────────┐     │
│  │        Zitadel                 │     │
│  │  (Complete IAM Platform)       │     │
│  │                                │     │
│  │  :8080 Web UI + APIs           │     │
│  │  - User Management             │     │
│  │  - OAuth/OIDC                  │     │
│  │  - Admin Console               │     │
│  │  - Multi-tenancy               │     │
│  └────────────┬───────────────────┘     │
│               │                          │
│               v                          │
│  ┌────────────────────────────────┐     │
│  │      PostgreSQL                │     │
│  │      :5432                     │     │
│  └────────────────────────────────┘     │
└─────────────────────────────────────────┘
```

## Services

### Zitadel
- **Web UI**: http://localhost:8080
- **Purpose**: Complete IAM platform with user management, OAuth/OIDC, and admin console
- **Database**: PostgreSQL on port 5432
- **Default Admin**: 
  - Username: `admin`
  - Password: `Password1!`

## Prerequisites

- Docker Desktop installed and running
- Docker Compose installed (included with Docker Desktop)

## Quick Start

### 1. Start Zitadel

```bash
cd poc-zitadel
docker-compose up -d
```

**Note:** First startup takes 2-3 minutes as Zitadel initializes the database.

### 2. Check services are running

```bash
docker-compose ps
```

You should see:
- zitadel (running)
- zitadel-db (healthy)

### 3. View logs

```bash
# All services
docker-compose logs -f

# Zitadel only
docker-compose logs -f zitadel
```

### 4. Access Zitadel

Open your browser: **http://localhost:8080**

Login with:
- **Username**: `admin`
- **Password**: `Password1!`

### 5. Stop services

```bash
docker-compose down
```

### 6. Stop and remove all data

```bash
docker-compose down -v
```

## Using Zitadel

### Access the Admin Console

1. Go to http://localhost:8080
2. Login with admin credentials
3. You'll see the Zitadel Console with:
   - **Organizations**: Manage tenants
   - **Projects**: Group applications
   - **Users**: User management
   - **Applications**: OAuth clients
   - **Settings**: Configuration

### Create Your First Application (OAuth Client)

1. In the Console, go to **Projects**
2. Click **Create New Project**
3. Enter project name (e.g., "My App")
4. Click on your project
5. Go to **Applications** tab
6. Click **New**
7. Choose application type:
   - **Web**: For web applications
   - **User Agent**: For SPAs
   - **Native**: For mobile apps
   - **API**: For machine-to-machine
8. Configure redirect URIs
9. Save and note the **Client ID** and **Client Secret**

### Create a User

1. In the Console, go to **Users**
2. Click **New**
3. Choose **Human** (regular user) or **Machine** (service account)
4. Fill in user details:
   - Email
   - First name
   - Last name
   - Username
5. Set initial password
6. Click **Create**

### Test OAuth Flow

**1. Get OIDC configuration:**
```bash
curl http://localhost:8080/.well-known/openid-configuration
```

**2. Start authorization flow:**
```
http://localhost:8080/oauth/v2/authorize?client_id=<CLIENT_ID>&redirect_uri=<REDIRECT_URI>&response_type=code&scope=openid%20profile%20email
```

**3. Exchange code for tokens:**
```bash
curl -X POST http://localhost:8080/oauth/v2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=<CODE>&redirect_uri=<REDIRECT_URI>&client_id=<CLIENT_ID>&client_secret=<CLIENT_SECRET>"
```

**4. Get user info:**
```bash
curl http://localhost:8080/oidc/v1/userinfo \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## API Endpoints

### OIDC/OAuth Endpoints
- `GET /.well-known/openid-configuration` - OIDC discovery
- `GET /oauth/v2/authorize` - Authorization endpoint
- `POST /oauth/v2/token` - Token endpoint
- `GET /oidc/v1/userinfo` - UserInfo endpoint
- `GET /oauth/v2/keys` - Public keys (JWKS)

### Management API
- `POST /management/v1/users/human/_search` - Search users
- `POST /management/v1/users/human` - Create user
- `GET /management/v1/users/{id}` - Get user
- `POST /management/v1/projects` - Create project
- `POST /management/v1/projects/{id}/apps/oidc` - Create OIDC app

### Admin API
- `POST /admin/v1/orgs` - Create organization
- `GET /admin/v1/orgs/{id}` - Get organization

Full API docs: http://localhost:8080/openapi (when running)

## Key Features

### 1. Multi-tenancy
- Create multiple **Organizations** (tenants)
- Each org has its own users and settings
- Isolate data per organization

### 2. Projects & Applications
- Group related applications in **Projects**
- Each project can have multiple apps
- Share users across apps in same project

### 3. Built-in User Management
- User registration and login
- Password policies
- Email verification
- MFA (TOTP, WebAuthn)
- Social login providers

### 4. Passwordless Authentication
- WebAuthn support
- FIDO2 security keys
- Biometric authentication

### 5. Event Sourcing
- All changes stored as immutable events
- Complete audit trail
- Point-in-time recovery

### 6. Branding & Customization
- Custom login pages
- White-label capabilities
- Custom domains
- Email templates

## Zitadel vs Hydra + Kratos

### Zitadel (All-in-One)
```
PROS (+)
  * Setup: Single service, easier to deploy and manage
  * UI: Built-in admin console and user management UI
  * Features: Complete IAM out-of-the-box (MFA, passwordless, etc.)
  * Multi-tenancy: Native support for organizations
  * Developer Experience: Modern APIs and excellent documentation
CONS (-)
  * Flexibility: Less control over individual components
  * Maturity: Newer project, smaller community
  * Customization: Less granular control than separate services
```

### Hydra + Kratos (Modular)
```
PROS (+)
  * Flexibility: Full control over each component
  * Maturity: Battle-tested in production
  * Customization: Build exactly what you need
  * Lightweight: Minimal footprint per service
CONS (-)
  * Complexity: Must integrate multiple services
  * Development: More code to write and maintain
  * UI: Must build all UI components yourself
```

## Use Cases

### Choose Zitadel When:
- You want a complete IAM solution quickly
- You need multi-tenancy out-of-the-box
- You prefer built-in admin UI
- You want passwordless authentication
- You're building a new system from scratch

### Choose Hydra + Kratos When:
- You need maximum control and flexibility
- You have existing user management
- You want minimal infrastructure footprint
- You have specific customization requirements
- You prefer modular architecture

## Troubleshooting

**Zitadel won't start:**
```bash
# Check logs
docker-compose logs zitadel

# Database might not be ready, wait 30 seconds and check again
docker-compose ps
```

**Can't login:**
- Default credentials: `admin` / `Password1!`
- Make sure Zitadel finished initialization (check logs)

**Port 8080 already in use:**
Edit `docker-compose.yml` and change:
```yaml
ports:
  - "8081:8080"  # Use 8081 instead
```

**Reset everything:**
```bash
docker-compose down -v
docker-compose up -d
```

## Resources

- [Zitadel Docs](https://zitadel.com/docs)
- [Zitadel GitHub](https://github.com/zitadel/zitadel)
- [API Documentation](https://zitadel.com/docs/apis/introduction)
- [Quickstart Guides](https://zitadel.com/docs/guides/start/quickstart)
- [Community Discord](https://zitadel.com/chat)

## Next Steps

1. **Explore the Console**: Navigate through organizations, projects, users
2. **Create an Application**: Set up an OAuth client for your app
3. **Test OAuth Flow**: Use the authorization endpoint to get tokens
4. **Integrate with Your App**: Use Zitadel SDKs or standard OAuth libraries
5. **Configure Branding**: Customize login pages and emails
6. **Set up MFA**: Enable multi-factor authentication for users
