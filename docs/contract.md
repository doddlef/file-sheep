# File Sheep — Backend Coding Contract

# 1. Purpose

This document defines the backend coding contract for **File Sheep**.

Its goal is to keep the codebase:

- consistent
- maintainable
- scalable
- AI-friendly
- easy to review and extend

All developers and AI-generated code should follow this contract unless there is a clear and documented reason not to.

This contract is especially important because File Sheep is developed with **vibe coding / AI-assisted development**.  
A stable contract reduces inconsistency, prevents layer leakage, and makes generated code safer to trust.

---

# 2. Core Principles

## 2.1 Readability over cleverness

Code should be easy to read, reason about, and review.

Avoid:

- overly clever abstractions
- hidden side effects
- magic behavior
- unnecessary generic frameworks

Prefer:

- explicit naming
- clear boundaries
- predictable flows
- small, understandable units

---

## 2.2 Composition over inheritance

Prefer **composition** rather than inheritance.

Inheritance should only be used in special and justified cases, such as:

- template pattern
- shared framework base classes
- controlled extension points with clear semantics

Do not introduce inheritance only to reduce a few duplicated lines.

Prefer:

- helper classes
- domain services
- delegated components
- interfaces + implementations

---

## 2.3 Interface and implementation should be separated

Service and infrastructure contracts should be defined as interfaces.

This improves:

- testability
- replaceability
- architecture clarity
- AI generation consistency

Typical pattern:

- `XxxService` → service interface
- `XxxServiceImpl` → service implementation
- `XxxRepo` → repository / infra interface
- `JooqXxxRepo` → jOOQ-based implementation
- `LocalFileEngine` / `S3FileEngine` → storage implementations

Controller layer does **not** require interfaces in normal cases.

---

## 2.4 Layer boundaries must be explicit

Each layer has clear responsibilities.

A layer must not absorb the responsibility of another layer.

Examples:

- controller should not contain business logic
- service should not directly expose HTTP concerns
- repo should not implement business rules
- infra should not decide product behavior
- controller DTO should not leak into service layer
- persistence models should not leak into API responses

---

## 2.5 Design for future extension, but do not over-engineer

Code should allow future growth, but must not become abstract too early.

Prefer:

- stable contracts
- replaceable components
- scalable parameter objects

Avoid:

- building v2/v3 complexity into every v1 feature
- speculative abstractions without a real use case

---

# 3. Layered Architecture

The backend should generally follow:

- controller layer
- service layer
- infra layer

Additional supporting packages may include:

- model / domain
- storage
- config
- common
- security

---

# 4. Controller Layer Contract

## 4.1 Responsibility

The controller layer is responsible for:

- receiving HTTP requests
- validating request format
- converting request DTOs into service-layer command/query objects
- calling service interfaces
- converting service results into response DTOs
- returning HTTP responses

The controller layer must remain thin.

---

## 4.2 What controller must not do

Controller must not:

- implement core business logic
- directly access database/repo
- directly manipulate persistence models
- directly use jOOQ or SQL
- directly use request DTO as service input
- contain large authorization logic beyond entry checks
- handle storage engine logic

---

## 4.3 Controller interface rule

Controllers normally do **not** need interfaces.

Use concrete controller classes such as:

- `AuthController`
- `DriveController`
- `ShareController`

---

## 4.4 Request DTO naming

For request bodies, use DTO names like:

- `RegisterRequest`
- `LoginRequest`
- `CreateFolderRequest`
- `MoveFileRequest`
- `UploadFileRequest`

DTOs represent API input only.

They belong to the controller/API boundary, not the service contract.

---

## 4.5 Controller should convert DTO to command/query object

Controller should never pass request DTO directly into the service layer.

Preferred flow:

`XxxRequest -> XxxCmd / XxxQuery -> service`

Example:

```kotlin
data class CreateFolderRequest(
    val parentFolderId: String?,
    val name: String
)

data class CreateFolderCmd(
    val operatorId: Long,
    val parentFolderId: Long?,
    val name: String
)
```

# 5. Service Layer Contract

## 5.1 Responsibility

The **service layer** is responsible for implementing the core business logic of the application.

Responsibilities include:

- implementing domain/business rules
- orchestrating operations across repositories and other services
- enforcing permission checks
- managing transactions
- coordinating with infrastructure components (repo, storage engine, email sender, etc.)
- transforming persistence data into domain results

The service layer acts as the **main application logic boundary**.

Controllers should delegate to services, and infrastructure should support services.

---

## 5.2 Service Interface and Implementation Naming

Service classes must follow a consistent interface + implementation structure.

Preferred naming:

| Role | Naming |
|-----|------|
| Service Interface | `XxxService` |
| Service Implementation | `XxxServiceImpl` |

Examples:

- `AuthService` → `AuthServiceImpl`
- `FolderService` → `FolderServiceImpl`
- `FileService` → `FileServiceImpl`
- `ShareService` → `ShareServiceImpl`

Interfaces define the **service contract**.  
Implementations contain the **actual logic**.

---

## 5.3 Service Parameter Style

When a service method requires **multiple parameters**, or may expand in the future, prefer using a **Value Object (VO)**.

Avoid long primitive parameter lists.

### Avoid

```kotlin
fun createFolder(userId: Long, parentFolderId: Long?, name: String)
```

### Prefer

```kotlin
fun createFolder(cmd: CreateFolderCmd): FolderDetail
```

Example command object:

```kotlin
data class CreateFolderCmd(
    val operatorId: Long,
    val parentFolderId: Long?,
    val name: String
)
```

## 5.4 Service Result Style

Service methods should return **domain-oriented results**, not controller DTOs.

Preferred return object naming:

- `XxxResult`
- `XxxDetail`
- `XxxView`

Examples:

- `LoginResult`
- `FolderDetail`
- `FileDetail`
- `ShareResult`

Avoid returning:

- controller response DTOs
- raw repository records
- persistence entities directly

Service results should represent **business-level information**, not persistence structure.

Example:

```kotlin
data class LoginResult(
    val userId: Long,
    val accessToken: String,
    val refreshToken: String
)
```

Controller converts the result into response DTO:

```kotlin
data class LoginResponse(
    val accessToken: String
)
```

## 5.5 Service Must Not Depend on Controller DTO

Service methods must never depend on request/response DTO classes defined in the controller layer.

DTOs belong to the API boundary, while service contracts represent application logic.

Correct example:

```kotlin
fun createFolder(cmd: CreateFolderCmd): FolderDetail
```

# 6. Infra Layer Contract

## 6.1 Responsibility

The infrastructure layer is responsible for low-level system interactions.

Responsibilities include:
- database persistence
- external system integration
- file storage engines
- email sending
- cache access
- third-party API integration

Infrastructure should **execute operations**, not implement business policy.

## 6.2 Infra Interface and Implementation Naming

Repositories should follow **interface + implementation** separation.

| Role                 | Naming      |
|----------------------|-------------|
| Repository Interface | XxxRepo     |
| jOOQ implementation  | JooqXxxRepo |

## 6.3 Repo Input Style

Repository methods should prefer query objects instead of long parameter lists.

Preferred naming patterns:
- XxxInsertQuery
- XxxUpdateQuery
- XxxDeleteQuery
- XxxListQuery

Example:

```kotlin
interface FileRepo {
    fun insert(query: FileInsertQuery): Int
}
```

Usage:

```kotlin
fileRepo.insert(
    FileInsertQuery(
        id = fileId,
        ownerId = cmd.operatorId,
        parentFolderId = cmd.parentFolderId,
        entityId = entityId,
        name = cmd.name,
        extension = extension
    )
)
```

# 7. Package Oraganization

Code should be organized primarily by **domain**, not purely by technical layer.

Avoid circular dependencies in packages/modules.

# 8. Exception Handling Rules

Use explicit business exceptions.

example hierarchy:

```kotlin
AppException
AuthException
PermissionDeniedException
ResourceNotFoundException
ConflictException
```

Guidelines:
- avoid throwing generic runtime exceptions
- express meaningful business failures
- map exceptions through exception handlers

# 9. Security Rules

Never Trust Client Ownership Fields: Authenticated identity must come from **security context**, not request body.

Controllers enforce authentication, but services enforce authorization.

Never expose:
- password hashes
- refresh tokens
- internal storage paths
- internal-only database fields

# 10. Testing Rules

Testing priorities:
1.  service-layer business logic
2.	repository integration tests
3.	controller API tests

Critical flows such as authentication, file upload, and permission enforcement should always have tests.

# 11. Example layer flow

example:: **Create Folder**

Controller
- receives CreateFolderRequest
- converts to CreateFolderCmd
- calls FolderService
Service
- validates ownership
- applies business rules
- calls repository

Repository
- persists data using FolderInsertQuery

# 12. Breaking the Contract

Breaking this contract is acceptable only when:
- required by framework constraints
- necessary for performance
- complexity is reduced
- the deviation is documented

Do not break the contract for convenience.

# 17. Summary

The File Sheep backend follows these principles:
- thin controllers
- business-focused services
- explicit infrastructure contracts
- DTO separated from service VO
- repository query objects
- composition preferred over inheritance
- interface + implementation separation
- domain-oriented package organization
- AI-friendly naming conventions