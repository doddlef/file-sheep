# File Sheep — Domain Overview

## 1. Introduction

File Sheep is a personal cloud file drive system designed to store, organize, and share digital files securely.

The system allows users to upload files, manage folders, share content, and access files from multiple clients (web, desktop, or API).  
It is designed with a clean backend architecture, storage abstraction, and security-first authentication, making it suitable for future cloud deployment and extensibility.

File Sheep separates logical file management from physical storage, allowing the system to support multiple storage engines such as local disk or cloud object storage (e.g., AWS S3).

The project is designed to demonstrate:

- modern backend architecture
- secure authentication design
- scalable file storage abstraction
- maintainable domain modeling
- AI-assisted development workflow


---

# 2. Core Design Philosophy

The system follows several key principles.

### Storage Abstraction
The system must not depend on a specific storage implementation.

Files are stored through a FileEngine interface, allowing the storage backend to change without affecting business logic.

Examples:
- Local disk storage
- AWS S3
- MinIO
- Other object storage systems

### Logical vs Physical File Separation

File Sheep separates logical file records from physical file entities.

This separation allows:

- file deduplication
- versioning
- storage migration
- storage engine independence

### Security First

All file operations must enforce strict ownership or permission checks.

The system is designed to ensure:

- authenticated access
- secure token lifecycle
- permission enforcement
- safe file access

### Cloud Ready Architecture

Although development begins locally using Docker and PostgreSQL, the architecture is designed for future deployment to cloud infrastructure.

---

# 3. Core Domain Concepts

## 3.1 User

A User represents a registered account in the system.

Users authenticate using email and password and gain access to a personal file drive.

Each user owns:

- a personal root folder
- files and folders created within their drive
- share links created from their files

Users are responsible for managing their own files and shares.

---

## 3.2 Folder

A Folder represents a directory used to organize files.

Folders form a hierarchical tree structure similar to a traditional filesystem.

Properties of a folder include:

- owner
- parent folder
- name
- creation time
- deletion state (for recycle bin support)

Folders may contain:

- subfolders
- files

Folders may also be shared.

---

## 3.3 File

A File represents a logical file visible in the user’s cloud drive.

A file is not the physical file itself, but rather a reference to stored content.

The file contains metadata such as:

- file name
- file extension
- owner
- parent folder
- timestamps
- reference to a storage entity

Multiple files may reference the same underlying stored entity in the future.

This abstraction enables future features such as deduplication and versioning.

---

## 3.4 Entity (Storage Object)

An Entity represents the actual stored binary file in the storage system.

Entities are responsible for describing how and where a file is stored.

Typical entity information includes:

- storage engine type
- storage key or path
- file size
- MIME type
- content hash (optional in early versions)
- reference count
- creation timestamp

The entity is managed by the FileEngine.

Entities are invisible to end users and exist only to support file storage.

---

## 3.5 Share

A Share allows a file or folder to be accessed by others.

The initial version of File Sheep supports share links that allow read access to a file or folder.

A share record typically includes:

- the resource being shared (file or folder)
- the owner
- a share token
- permissions
- optional expiration time

Shares allow users to distribute access to files without exposing the entire drive.

Future versions may support:

- user-to-user sharing
- edit permissions
- workspace collaboration

---

## 3.6 Recycle Bin

When files or folders are deleted, they are not immediately removed from the system.

Instead, they are moved to the Recycle Bin.

The recycle bin allows users to:

- restore deleted files
- prevent accidental data loss

Physical deletion of stored entities may occur later through background cleanup processes.

---

# 4. Storage Engine

File Sheep uses a FileEngine abstraction to interact with file storage.

The FileEngine defines operations such as:

- saving files
- retrieving files
- deleting stored objects
- verifying file existence

Concrete implementations may include:

- LocalFileEngine (local filesystem storage)
- S3FileEngine (AWS S3 object storage)
- other storage systems

The application layer interacts only with the FileEngine interface, ensuring that storage can be replaced without affecting business logic.

---

# 5. Domain Relationships

The primary relationships between domain objects are:

User  
→ owns → Folder

Folder  
→ contains → Folder

Folder  
→ contains → File

File  
→ references → Entity

Share  
→ references → File or Folder

Entity  
→ represents → stored binary file

This design separates logical organization from physical storage.

---

# 6. Future Domain Extensions

The architecture supports future features such as:

- chunked and resumable uploads
- file deduplication
- version history
- team workspace drives
- collaborative sharing
- storage migration
- audit logging

The current domain model is intentionally designed to support these extensions without requiring major redesign.

---

# 7. Summary

File Sheep models a cloud drive system through a clear separation of concerns:

- User manages the account
- Folder organizes files
- File represents logical user-visible files
- Entity stores the physical binary content
- Share controls access to resources
- FileEngine abstracts the storage backend

This domain design provides a strong foundation for building a secure, maintainable, and cloud-ready file storage system.