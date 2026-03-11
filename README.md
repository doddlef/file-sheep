# BoxDrive

BoxDrive is a modern cloud-based file box platform designed for personal file sharing and lightweight collaboration.

The system allows users to create **boxes** that contain files and share them with others through memberships or access links. Unlike traditional cloud drives with deep folder hierarchies, BoxDrive uses a **flat box model** to simplify sharing and collaboration while still supporting scalable storage and permission control.

The project is designed as a **production-style system** with emphasis on:

- clean architecture
- maintainability
- scalability
- clear domain modeling
- modern backend engineering practices

This repository is primarily built as a **learning-focused engineering project** while also serving as a **portfolio-quality system** demonstrating full-stack and distributed system design.

---

## Project Goals

The main goals of this project are:

1. **Build a maintainable cloud storage backend**
2. **Design a scalable domain model for sharing and collaboration**
3. **Practice modern backend architecture**
4. **Implement production-style engineering practices**
5. **Create a strong resume project demonstrating system design ability**

The system is intentionally designed with **future extensibility in mind**, allowing the project to evolve into a more collaborative workspace.

---

## Key Concepts

### Boxes

A **Box** is the core unit of organization.

Each box contains a **flat list of files** rather than nested folders.  
This simplifies collaboration and sharing workflows.

Examples:

- `CS Notes`
- `Homework`
- `Project Materials`

---

### Membership

Users can be invited into boxes with roles:

| Role | Permissions |
|-----|-------------|
| VIEWER | View and download files |
| CONTRIBUTOR | Upload new files |
| EDITOR | Modify file metadata and delete files |
| OWNER | Full control of the box |

---

### Sharing

Boxes may be shared through several mechanisms:

- **Membership invitations**
- **Public visibility**
- **Share/access links (future)**

---

### Visibility

Boxes may be:

- `PRIVATE` – only accessible by members
- `PUBLIC` – readable by anyone with the direct URL

---

### File Storage

Files are stored through a **logical file + storage entity separation**:

- logical file record → user-facing metadata
- storage entity → physical file stored in a storage engine

This enables:

- future deduplication
- file cloning
- multiple storage backends

---

## Architecture Goals

This project aims to follow modern backend practices:

- Domain-driven structure
- Clear separation of layers
- Storage abstraction
- Extensible permission model
- Scalable storage design

The architecture will support:

- multiple storage engines
- scalable file storage
- future collaboration features
- cloud deployment

---

## Future Features

Planned future expansions include:

- share links
- preview support (images, videos, text)
- markdown editing
- realtime collaborative editing
- file versioning
- storage quotas
- activity logs
- public discovery of boxes
- distributed storage

---

## Status

Early development.

Current focus:

- authentication
- box domain
- file upload and management