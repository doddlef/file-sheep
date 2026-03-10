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

The drive domain represents the cloud file system.

### 2.1 folders

Represents a directory in a user's cloud drive, form a hierarchical tree structure.

#### Fields
- `id`: uuid pk
- `owner_id`:: id of the user who owns the folder
- `parent_id`: optional id of the parent folder
  - null iff the folder is a root folder
- `name`: text: name of the folder
- `deleted_at`: optional timestamp: when the folder was deleted
- `created_at`: timestamp:
- `updated_at`: timestamp:

#### Notes
- a root folder is created automatically when a user is created
- `parent_id` is null iff the folder is a root folder
- `deleted_at` is set when the folder/its parent is moved to the trash

### 2.2 entities

Represents the **actual stored file content**. An entity is managed by the storage engine.

Entities are **not directly visible to users**.

#### Fields
- `id`:
- `engine`: text:
- `source`: text: repesensts the path or object key in storage
- `size`: bigint:
- `referenced_count`: int:
- `mime_type`: text:
- `hash`: text:
- `created_at`:

#### Notes
- `engine` indicates the storage engine (e.g., local, s3, gcs)
- `source` represents the path or object key in storage engine.
- Multiple logical file may reference one entity.

### 2.3 files

Represents a **logical** file record in the user’s drive. The file references a stored entity.

#### Fields
- `id`:
- `owner_id`:: id of the user who owns the file
- `folder_id`:: id of the folder the file belongs to
- `entity_id`:: id of the entity the file references
- `name`: text:
- `extension`:
- `meta`: optional jsonb: optional metadata
- `deleted_at`:
- `created_at`:
- `updated_at`:

#### Notes
- `deleted_at` is set when the file/its parent is moved to the trash
- Actual file content is stored in `entities` table.
- `extension` is optional and is used to determine the file type.

## Future Work
- `trash` table
- `share` related tables