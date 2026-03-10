# AI Prompt — Secure Authentication Module with Refresh Session Rotation

## Objective

Generate a **secure authentication module** that implements:

- **JWT access tokens**
- **Rotating refresh tokens**
- **Database-backed refresh sessions**
- **Replay / reuse detection**
- **Hashed refresh token storage**

The goal is to produce **production-grade authentication logic**.

The generated implementation **does not need to use exact function names or classes described here**, but the **security model and workflows must remain consistent**.

---

# Architecture Requirements

Use a **layered architecture** similar to:

Controller → Service → Repository → Database

Typical responsibilities:

### Controller
Handles HTTP endpoints:

- login
- refresh
- logout

### Service Layer

Split responsibilities logically.

Possible services:

**Token Service**
Responsible for:

- issuing access tokens
- parsing / validating access tokens
- login workflow
- refresh workflow
- logout workflow

**Refresh Session Service**

Responsible for:

- creating refresh sessions
- loading sessions (with row locking)
- rotating refresh tokens
- revoking sessions
- validating refresh token hashes

### Repository Layer

Responsible for:

- storing refresh sessions
- updating token hashes
- revoking sessions
- locking session rows during refresh

Database access implementation can use any ORM or SQL layer.

---

# Token Model

## Access Token

Access tokens must be:

- JWT
- short lived
- stateless
- not stored in database

Typical properties:

- expiration: ~10–20 minutes
- signed by server secret
- contain user identity and minimal claims

Sent via:

```
Authorization: Bearer <access_token>
```

---

## Refresh Token

Refresh tokens must:

- be long random secrets
- be stored **hashed in database**
- never store raw refresh token
- rotate after each refresh

Refresh token format:

```
<sessionId>.<secret>
```

Example:

```
550e8400-e29b-41d4-a716-446655440000.randomSecretValue
```

Where:

- `sessionId` identifies the refresh session
- `secret` is the random refresh token secret

---

# Hashing Requirement (IMPORTANT)

The database **must never store the raw refresh token**.

Instead:

Store:

```
current_token_hash
previous_token_hash
```

Hashing can use:

- HMAC-SHA256(secret, serverPepper)
- or another strong deterministic hash

Reason:

- refresh tokens are already high-entropy random values
- deterministic hashing allows lookup and comparison

---

# Security Rules

The implementation must enforce the following:

1. **Access tokens are short-lived**

2. **Refresh tokens are hashed in database**

3. **Refresh tokens rotate after every successful refresh**

4. **Old refresh token should not remain valid indefinitely**

5. **Refresh requests must lock session row**

    Use transactional logic or row-level locking:
    
    ```
    SELECT ... FOR UPDATE
    ```

6. **Token reuse detection**

    If a refresh token is reused incorrectly, the session must be revoked.

7. **Refresh tokens must be delivered via secure cookie**

Use cookie properties:

- HttpOnly
- Secure (production)
- SameSite Lax or Strict

---

# Workflows

## Login Flow

When user logs in successfully:

1. Validate credentials
2. Create a new refresh session
3. Generate random refresh secret
4. Hash refresh secret
5. Store hash in database
6. Generate access token (JWT)
7. Return:

- access token and refresh token pair in response
- refresh token cookie

Pseudo flow:

```
authenticate user

sessionId = generate id
secret = generate random token

hash = hash(secret)

store refresh_session(
  sessionId,
  accountId,
  current_token_hash = hash
)

accessToken = sign JWT

refreshToken = sessionId + "." + secret

return accessToken + refresh cookie
```

---

## Access Token Validation

For protected API endpoints:

1. Extract access token from Authorization header
2. Validate JWT signature
3. Validate expiration
4. Extract user identity
5. Build authenticated principal

No database lookup required.

---

## Refresh Flow

When access token expires:

1. Client sends refresh token cookie
2. Parse token:

    ```
    sessionId.secret
    ```

3. Load refresh session **with row lock**

4. Validate:

   - session exists
   - session not revoked
   - session not expired

5. Hash incoming secret

6. Compare hash

### Case A — matches current_token_hash

Refresh is valid.

Rotate token:

```
newSecret = random()
newHash = hash(newSecret)

previous_token_hash = current_token_hash
current_token_hash = newHash
```

Update:

```
last_used_at
last_rotated_at
```

Issue:

- new access token
- new refresh token

---

### Case B — matches previous_token_hash within grace window

Possible race condition.

Optionally allow request without revoking session.

Grace window example:

```
5–30 seconds
```

---

### Case C — no match

Treat as suspicious token reuse.

Action:

```
revoke session
reject request
```

---

## Logout Flow

Logout must invalidate the refresh session.

Steps:

1. Read refresh token cookie
2. Parse sessionId
3. Load session
4. Mark session revoked

Example:

```
revoked_at = now
revoke_reason = "logout"
```

Clear refresh cookie.

---

# Session Lifetime Rules

Two lifetime limits should exist.

### Access Token Lifetime

Example:

```
15 minutes
```

### Refresh Session Idle Timeout

Example:

```
7 days
```

### Refresh Session Absolute Lifetime

Example:

```
30 days
```

Refresh requests must validate both limits.

---

# Cookie Requirements

Refresh token cookie must use:

```
HttpOnly = true
Secure = true (production)
SameSite = Lax or Strict
```

Example cookie:

```
name: refresh_token
path: /auth
```

---

# Implementation Notes

The generated code should:

- separate controller / service / repository layers
- keep refresh token logic server-side
- avoid storing raw refresh tokens
- use transactional refresh logic
- ensure refresh rotation
- include session revocation capability

Do not:

- store raw refresh tokens
- allow refresh without session validation
- allow refresh token reuse
- expose refresh token to JavaScript

---

# Expected Result

Generate code for a **secure authentication system** that includes:

- login endpoint
- refresh endpoint
- logout endpoint
- JWT access token handling
- refresh session storage
- refresh token rotation
- hashed refresh token storage
- reuse detection
- secure cookie handling

The implementation language and frameworks may vary, but the **security workflow must match this specification**.