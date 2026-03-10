# AI Prompt — Email Registration with Verification (Service Layer Focus)

## Objective

Generate a **secure email registration workflow** focused on the **Service Layer**.

The system must:

- require **email verification before account creation**
- avoid inserting **unverified users into the database**
- store **temporary registration state in Redis**
- store **hashed verification codes only**
- support **cooldown and retry limits**
- minimize sensitive data stored in Redis
- rely on **numeric verification codes (e.g. `1234`)**

This prompt focuses primarily on the **Service layer design**, but may include **Controller and Repository hints** for context.

Project namespace:

```
dev.haomin.filesheep.auth
```

Configuration properties:

```
dev.haomin.filesheep.auth.prop.RegisterProperties
```

---

# Architecture Overview

Layered architecture:

```
Controller → Service → Repository → Database / Redis
```

The main responsibility is in the **Service layer**.

---

# Configuration

Registration behavior must be configurable via:

```
RegisterProperties
```

Example values may include:

```
verificationCodeLength
verificationCodeTtl
resendCooldown
maxVerifyAttempts
attemptTtl
```

The service layer should **read configuration values from this class** instead of hardcoding them.

---

# Validation Utilities

Input validation should use a reusable helper, put it under the `common.utils` package:

```
ValidatorUtils
```

Suggested methods:

```
ValidatorUtils.validateEmail(email)
ValidatorUtils.validateNickname(nickname)
ValidatorUtils.validatePassword(password)
```

Responsibilities:

### Email validation

- non-null
- trimmed
- valid email format

### Nickname validation

- non-blank
- length limits
- allowed characters
- trimmed

### Password validation

- minimum length
- maximum length
- not blank

The service layer should call these validators before continuing the workflow.

---

# Storage Model

## Persistent Database

Only **fully verified accounts** should be stored in the database.

Example repository:

```
AccountRepo
```

Methods may include:

```
selectByEmail(email)
insertAccount(...)
```

Important rule:

```
email must have UNIQUE constraint
```

---

## Temporary Registration State (Redis)

All temporary registration attempts must be stored in Redis.

Suggested key patterns:

```
register:attempt:{attemptId}
register:email:{email}
register:cooldown:{attemptId}
```

Attempts must have a **TTL**.

Example:

```
10 minutes
```

Expired attempts are treated as invalid.

---

# Registration Attempt Model

Service layer should use a structure similar to:

```
RegisterAttempt
```

Fields:

```
attemptId
email
codeHash
verifiedAt
tryCount
createdAt
```

Important rules:

```
DO NOT store raw verification code
```

Only store:

```
codeHash
```

---

# Verification Code Design

At this stage the system should use:

```
numeric verification code
```

Example:

```
1234
```

Code length should be configurable using:

```
RegisterProperties.verificationCodeLength
```

---

# Code Hashing

Before storing verification code:

```
codeHash = HMAC_SHA256(code, serverPepper)
```

Only `codeHash` should be stored in Redis.

When user submits verification code:

```
hash(submittedCode) == storedHash
```

---

# Service Layer Responsibilities

Main service:

```
RegisterService
```

Implementation:

```
RegisterServiceImpl
```

Responsibilities:

```
sendVerificationEmail
verifyCode
completeRegistration
resendVerification
```

---

# Service Flow

## 1. Send Verification Email

Service method example:

```
sendVerificationEmail(email)
```

Steps:

1. Normalize email

    ```
    email = email.trim().lowercase()
    ```

2. Validate email

    ```
    ValidatorUtils.validateEmail(email)
    ```

3. Check if this account already exists

    ```
    accountRepo.selectByEmail(email)
    ```

    If an account exists:
    
    - optionally return a generic response to avoid enumeration.

4. Check if an active attempt already exists

    Use Redis key:
    
    ```
    register:email:{email}
    ```
    
    If exists:
    
    - return existing attemptId
      - do not send another email immediately.

5. Generate:

    ```
    attemptId
    verificationCode
    ```
    
    Verification code must be numeric.
    
    Example:
    
    ```
    Random numeric code (length defined in properties)
    ```

6. Hash verification code

    ```
    codeHash = hash(code)
    ```

7. Store attempt in Redis:

    ```
    attemptId
    email
    codeHash
    tryCount = 0
    verifiedAt = null
    createdAt
    ```
    
    Set TTL using:
    
    ```
    RegisterProperties.attemptTtl
    ```

8. Store email → attemptId mapping

9. Send verification email

    Important rule:
    
    ```
    DO NOT log verification code
    ```
    
    Response:
    
    ```
    attemptId
    expiresIn
    ```

---

# 2. Verify Email Code

Service method example:

```
verifyCode(attemptId, code)
```

Steps:

1. Load attempt from Redis

    If missing:
    
    ```
    throw invalid or expired attempt
    ```

2. Check expiration

3. Check try count

    Example limit:
    
    ```
    RegisterProperties.maxVerifyAttempts
    ```

4. Hash submitted code

5. Compare with stored hash

    If mismatch:
    
    - increment tryCount
      - save attempt
      - reject request
    
    If match:

6. Mark verified:

    ```
    verifiedAt = now
    ```

7. Remove `codeHash` (recommended)

    This reduces sensitive data stored in Redis.

8. Save attempt.

    Response:
    
    ```
    verification successful
    ```

---

# 3. Complete Registration

Service method example:

```
completeRegistration(attemptId, nickname, password)
```

Steps:

1. Validate nickname and password

    ```
    ValidatorUtils.validateNickname(nickname)
    ValidatorUtils.validatePassword(password)
    ```

2. Load attempt from Redis

3. Check:

    ```
    verifiedAt != null
    ```
    
    If not verified:
    
    ```
    reject request
    ```

4. Insert account:

    ```
    email
    nickname
    passwordHash
    ```
    
    Password must be hashed using secure password hashing.
    
    Examples:
    
    ```
    bcrypt
    argon2
    ```

5. Handle duplicate email race condition

    Catch database unique constraint error.

6. Delete attempt from Redis.

7. Delete email mapping.

    Response:
    
    ```
    account successfully created
    ```

---

# 4. Resend Verification

Service method example:

```
resendVerification(attemptId)
```

Steps:

1. Load attempt

2. Check resend cooldown:

    ```
    register:cooldown:{attemptId}
    ```

    If cooldown active:
    
    ```
    reject request
    ```

3. Generate new verification code

4. Hash new code

5. Update attempt
    
    ```
    codeHash
    tryCount reset
    ```

6. Send email

7. Set cooldown.

---

# Logging Rules

Logs must never contain:

```
verification code
password
refresh token
```

Logs may include:

```
attemptId
masked email
status
```

Example masked email:

```
ha****@gmail.com
```

---

# Security Goals

This design ensures:

- no unverified users stored in DB
- minimal sensitive data in Redis
- verification codes hashed
- temporary attempts expire automatically
- brute-force protection via try limits
- resend cooldown
- strong validation of user inputs

---

# Expected Generated Code

The AI should generate code that includes:

Service Layer:

```
RegisterService
RegisterServiceImpl
```

Supporting components:

```
ValidatorUtils
RegisterAttempt model
Redis access helper
AccountRepo
```

Key service methods:

```
sendVerificationEmail
verifyCode
completeRegistration
resendVerification
```

The implementation must follow the **secure workflow defined above** while keeping the **service layer as the main location for business logic**.