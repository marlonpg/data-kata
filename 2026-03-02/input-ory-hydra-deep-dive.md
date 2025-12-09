# Ory Hydra Deep Dive

## The Ory Ecosystem

Ory provides a suite of open-source identity and access management tools that work together to create a complete IAM solution.

### Ory Products Overview

**Ory Hydra** - OAuth 2.0 & OpenID Connect Server
- Handles token issuance and OAuth flows
- Manages OAuth clients
- Provides consent management
- Does NOT handle user management

**Ory Kratos** - Identity & User Management
- User registration and login
- User profiles and credentials
- Password reset and email verification
- Multi-factor authentication
- Social login integrations
- Account recovery flows

**Ory Oathkeeper** - Identity & Access Proxy
- API gateway and reverse proxy
- Request authentication and authorization
- Token validation and transformation
- Access control rules

**Ory Keto** - Permission & Authorization
- Fine-grained permissions (Google Zanzibar-style)
- Relationship-based access control
- Complex authorization policies

## Hydra + Kratos: The Complete Solution

When you combine Hydra and Kratos, you get a full-featured identity platform:

### What Kratos Provides (fills Hydra's gaps):
```
USER MANAGEMENT
  * User registration with email/password
  * User profiles and metadata
  * Password hashing and validation
  * Email verification workflows
  * Password reset and recovery
  * Account settings management
  * Multi-factor authentication (TOTP, WebAuthn)
  * Social login (Google, GitHub, Facebook, etc.)

USER INTERFACE
  * Self-service flows (login, registration, recovery)
  * Customizable UI templates
  * Account management pages
  * Profile update forms
```

### What Hydra Provides (OAuth/OIDC layer):
```
TOKEN MANAGEMENT
  * OAuth 2.0 access tokens
  * OpenID Connect ID tokens
  * Refresh token management
  * Token validation and expiration
  * Automatic key rotation

OAUTH FLOWS
  * Authorization code flow
  * Client credentials flow
  * Implicit flow
  * PKCE support
  * Consent management
```

## Architecture: Hydra + Kratos Together

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │
       ├─────────────────┐
       │                 │
       v                 v
┌─────────────┐   ┌─────────────┐
│ Ory Kratos  │   │ Ory Hydra   │
│  (Identity) │   │  (OAuth)    │
└──────┬──────┘   └──────┬──────┘
       │                 │
       v                 v
┌─────────────┐   ┌─────────────┐
│   User DB   │   │  OAuth DB   │
│  (Postgres) │   │  (Postgres) │
└─────────────┘   └─────────────┘
```

### The Flow with Hydra + Kratos:

1. User clicks "Login" in your app
2. App redirects to Hydra's OAuth authorization endpoint
3. Hydra redirects to your login UI (served by Kratos)
4. User enters credentials, Kratos validates them
5. Kratos tells Hydra "user is authenticated"
6. Hydra shows consent screen (if needed)
7. User consents, Hydra issues tokens
8. App receives tokens and validates them

## Hydra Deep Dive

### Core Concepts

**OAuth 2.0 Clients**
- Applications that want to access user resources
- Each client has a client_id and client_secret
- Configured with allowed redirect URIs and grant types

**Consent**
- User explicitly grants permission to clients
- "App X wants to access your profile and email"
- Hydra manages consent sessions and remembers choices

**Token Types**
- **Access Token**: Short-lived, used to access APIs
- **ID Token**: Contains user identity information (OIDC)
- **Refresh Token**: Long-lived, used to get new access tokens

### Hydra Configuration

**Key Settings:**
```yaml
urls:
  self:
    issuer: https://auth.example.com
  consent: https://example.com/consent
  login: https://example.com/login

ttl:
  access_token: 1h
  refresh_token: 720h
  id_token: 1h

secrets:
  system:
    - your-secret-key-here

oauth2:
  expose_internal_errors: false
  
oidc:
  subject_identifiers:
    supported_types:
      - public
      - pairwise
```

### Hydra APIs

**Admin API** (Internal use only)
- Create/update/delete OAuth clients
- Manage consent and login sessions
- Introspect tokens
- Revoke tokens

**Public API** (Exposed to clients)
- OAuth 2.0 authorization endpoint
- Token endpoint
- UserInfo endpoint (OIDC)
- JWK endpoint (public keys)
- Token revocation endpoint

### OAuth Flows in Detail

**Authorization Code Flow** (Most common)
```
1. Client redirects user to Hydra's /oauth2/auth
2. Hydra redirects to your login page
3. User authenticates, you accept login challenge
4. Hydra redirects to your consent page
5. User consents, you accept consent challenge
6. Hydra redirects back to client with authorization code
7. Client exchanges code for tokens at /oauth2/token
```

**Client Credentials Flow** (Machine-to-machine)
```
1. Client sends client_id and client_secret to /oauth2/token
2. Hydra validates credentials
3. Hydra returns access token (no user involved)
```

### Security Features

**Key Rotation**
- Automatic rotation of signing keys
- Multiple keys supported simultaneously
- Old tokens remain valid during rotation

**Token Introspection**
- Validate tokens without checking signatures
- Get token metadata and status
- Check if token is active or revoked

**PKCE (Proof Key for Code Exchange)**
- Protects authorization code flow
- Required for public clients (mobile/SPA)
- Prevents authorization code interception

## Kratos Deep Dive

### Core Concepts

**Identities**
- Represents a user in the system
- Contains traits (email, name, etc.)
- Can have multiple credentials (password, social, etc.)

**Self-Service Flows**
- Registration: Create new account
- Login: Authenticate existing user
- Recovery: Reset forgotten password
- Verification: Verify email address
- Settings: Update profile or password

**Identity Schema**
- JSON Schema defining user attributes
- Customizable per your requirements
- Validates user data on registration/update

### Kratos Configuration

**Key Settings:**
```yaml
selfservice:
  default_browser_return_url: https://example.com/
  
  flows:
    registration:
      enabled: true
      ui_url: https://example.com/registration
      
    login:
      ui_url: https://example.com/login
      
    recovery:
      enabled: true
      ui_url: https://example.com/recovery
      
    verification:
      enabled: true
      ui_url: https://example.com/verification

identity:
  default_schema_id: default
  schemas:
    - id: default
      url: file:///etc/config/identity.schema.json

courier:
  smtp:
    connection_uri: smtp://user:pass@smtp.example.com:587
```

### Identity Schema Example

```json
{
  "$id": "https://example.com/identity.schema.json",
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "User",
  "type": "object",
  "properties": {
    "traits": {
      "type": "object",
      "properties": {
        "email": {
          "type": "string",
          "format": "email",
          "title": "Email",
          "ory.sh/kratos": {
            "credentials": {
              "password": {
                "identifier": true
              }
            },
            "verification": {
              "via": "email"
            }
          }
        },
        "name": {
          "type": "string",
          "title": "Full Name"
        }
      },
      "required": ["email"],
      "additionalProperties": false
    }
  }
}
```

### Kratos Self-Service Flows

**Registration Flow:**
```
1. User visits /self-service/registration/browser
2. Kratos creates flow and returns flow ID
3. User submits registration form to Kratos
4. Kratos validates data against identity schema
5. Kratos creates identity and sends verification email
6. User is redirected to success page
```

**Login Flow:**
```
1. User visits /self-service/login/browser
2. Kratos creates login flow
3. User submits credentials
4. Kratos validates credentials
5. Kratos creates session
6. User is redirected with session cookie
```

### Social Login Integration

Kratos supports OIDC-based social logins:

```yaml
selfservice:
  methods:
    oidc:
      enabled: true
      config:
        providers:
          - id: google
            provider: google
            client_id: your-google-client-id
            client_secret: your-google-client-secret
            mapper_url: file:///etc/config/oidc.google.jsonnet
            scope:
              - email
              - profile
          
          - id: github
            provider: github
            client_id: your-github-client-id
            client_secret: your-github-client-secret
            mapper_url: file:///etc/config/oidc.github.jsonnet
            scope:
              - user:email
```

## Deployment Considerations

### Database Requirements

**Hydra:**
- PostgreSQL, MySQL, CockroachDB, or SQLite
- Stores OAuth clients, consent sessions, tokens
- Requires migrations on updates

**Kratos:**
- PostgreSQL, MySQL, CockroachDB, or SQLite
- Stores identities, credentials, sessions
- Requires migrations on updates

### Scaling

**Horizontal Scaling:**
- Both Hydra and Kratos are stateless
- Can run multiple instances behind load balancer
- Share database across instances

**Performance:**
- Hydra: Handles 1000s of requests per second
- Kratos: Optimized for self-service flows
- Use Redis for session storage in high-traffic scenarios

### High Availability

```
┌──────────────┐
│ Load Balancer│
└──────┬───────┘
       │
   ┌───┴────┬────────┬────────┐
   │        │        │        │
   v        v        v        v
┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
│Hydra│ │Hydra│ │Kratos│Kratos│
└──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘
   │       │       │       │
   └───────┴───────┴───────┘
           │
           v
    ┌──────────────┐
    │  PostgreSQL  │
    │   (Primary)  │
    └──────┬───────┘
           │
           v
    ┌──────────────┐
    │  PostgreSQL  │
    │  (Replica)   │
    └──────────────┘
```

## Hydra vs Auth0 - Technical Comparison

### Token Issuance

**Hydra:**
- Full control over token claims
- Custom token lifetime per client
- Support for custom grant types
- Manual key rotation or automatic

**Auth0:**
- Predefined token structure
- Limited claim customization via rules
- Fixed token lifetimes (with limits)
- Automatic key rotation

### Customization

**Hydra:**
- Build your own login/consent UI
- Complete control over user flows
- Custom authentication logic
- Any database schema

**Auth0:**
- Use hosted login pages
- Limited flow customization
- Rules/hooks for custom logic
- Fixed user schema with metadata

### Integration Complexity

**Hydra:**
- Requires understanding OAuth/OIDC
- Must implement login/consent endpoints
- Manual integration with identity providers
- More code to write and maintain

**Auth0:**
- SDK-based integration
- Hosted pages work out-of-the-box
- Pre-built social integrations
- Less code, faster implementation

## When to Use Hydra + Kratos

### Good Fit:
- You need complete control over user experience
- You have specific compliance requirements (data residency)
- You want to avoid per-user pricing
- You have development resources for integration
- You need custom authentication flows
- You're building a platform with multiple tenants

### Not a Good Fit:
- You need to launch quickly (< 2 weeks)
- You lack OAuth/OIDC expertise
- You need extensive third-party integrations
- You prefer fully managed services
- You have limited development resources

## Resources

**Official Documentation:**
- Hydra: https://www.ory.sh/hydra/docs/
- Kratos: https://www.ory.sh/kratos/docs/

**Community:**
- GitHub: https://github.com/ory
- Slack: https://slack.ory.sh/
- Forum: https://community.ory.sh/

**Examples:**
- Hydra + Kratos integration: https://github.com/ory/hydra-login-consent-node
- Self-service UI: https://github.com/ory/kratos-selfservice-ui-node
