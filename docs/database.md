# Database

This document defines the **conceptual database schema** for File Sheep.

It describes:
- core database tables
- key fields
- relationships between entities
- domain responsibilities

The purpose of this document is to provide a stable domain-level schema before generating SQL migrations.

Actual SQL definitions (DDL, indexes, constraints) will be generated later using Flyway migrations.

This document focuses on **data modeling**, not database syntax.

## 1. Auth Domain

### 1.1 accounts

Represents a registered account in the system. 

Each user owns a personal drive.

#### Account Status Enum
- `ACTIVE`: user is active and can log in
- `INACTIVE`: user is inactive and cannot log in, may be archived
- `BANNED`: user is banned and cannot log in

#### Fields
- `id`: uuid pk
- `email`: unique citext: user's primary email
- `nick`: text: user's nickname
- `password`: text: password hash
- `status`: AccountStatus: current account status
- `avatar`: optional avatar url

- `created_at`: timestamp: when the account was created
- `updated_at`: timestamp: when the account was last updated, automatically updated by triggers

#### Notes
- `email` must be unique
- `password` must be stored as secure hashes.

### 1.2 refresh_sessions

represents a refresh token session for authentication.

#### Fields
- `id`: uuid pk
- `account_id`: uuid
- `token`: text: hash of the current active refresh token
- `prev_token`: optional text: hash of the last used refresh token for rotation
- `last_used_at`: optional timestamp: when the refresh session was last rotated
- `revoked_at`: optional timestamp: when the refresh session was revoked
- `revoke_reason`: optional text: reason for revocation

- `created_at`:
- `updated_at`:

#### Notes
- Tokens are stored as hashes for security (never store refresh tokens in plain text)
- Used to support refresh token rotation
- allows logout and session revocation, including detection for token reuse
- `revoke_reason` can only be set when the token is revoked, `revoked_at` is set

## 2. Drive Domain

