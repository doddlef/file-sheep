# Domain Design

This document defines the core domain model of the BoxDrive system.

The system is designed to be:

-   maintainable
-   scalable
-   extensible
-   suitable for production-style backend architecture

The domain is intentionally designed to allow the project to evolve from
a **personal sharing tool** into a **collaborative workspace platform**.

------------------------------------------------------------------------

# System Overview

The core unit of the system is a **Box**.

A box is a container of files that can be shared with other users.

Unlike traditional cloud drives, boxes do not support nested folders.\
Each box contains a **flat list of files**.

This design simplifies collaboration, sharing, and permission
management.

------------------------------------------------------------------------

# Access Model

Access to a box may be granted through four mechanisms:

1.  Ownership
2.  Membership
3.  Visibility
4.  Share links (future)

Access is resolved in the following order:

1.  Owner
2.  Membership
3.  Box visibility
4.  Access/share link

------------------------------------------------------------------------

# Box Visibility

Boxes support two visibility modes.

## PRIVATE

The box is only accessible through:

-   ownership
-   membership
-   invitation links

Anonymous users cannot access the box.

------------------------------------------------------------------------

## PUBLIC

The box can be accessed by anyone through its direct URL.

Anonymous users are allowed **read-only access**.

They may:

-   view box contents
-   download files

Anonymous users cannot:

-   upload files
-   edit files
-   manage members
-   modify system state

------------------------------------------------------------------------

# Membership

Membership grants authenticated users permissions within a box.

Each membership links:

box_id + account_id + role

Each user may have **at most one membership per box**.

------------------------------------------------------------------------

## Membership Roles

  Role          Capabilities
  ------------- -------------------------------------------
  VIEWER        View box contents, download files
  CONTRIBUTOR   Upload files
  EDITOR        Rename files, edit metadata, delete files
  OWNER         Full control of the box

------------------------------------------------------------------------

### Permission hierarchy

VIEWER \< CONTRIBUTOR \< EDITOR \< OWNER

------------------------------------------------------------------------

### Role responsibilities

#### VIEWER

-   browse files
-   download files
-   preview files (future)

#### CONTRIBUTOR

Includes viewer permissions plus:

-   upload new files

#### EDITOR

Includes contributor permissions plus:

-   rename files
-   edit file metadata
-   delete files
-   edit file content (future)

#### OWNER

Includes editor permissions plus:

-   edit box metadata
-   manage members
-   change visibility
-   delete box
-   transfer ownership (future)

------------------------------------------------------------------------

# Box Structure

Each box contains:

-   metadata
-   files
-   members

Boxes do **not support nested folders**.

All files are stored in a flat structure inside the box.

------------------------------------------------------------------------

# File Model

The file system uses a **logical file model**.

Two separate entities are used.

## File

Represents the logical file in a box.

Contains:

-   filename
-   metadata
-   owner
-   box reference

## Entity

Represents the **physical stored file content**.

Entities are managed by the storage engine.

Multiple logical files may reference the same entity in the future.

This enables:

-   deduplication
-   cloning
-   versioning

------------------------------------------------------------------------

# Sharing Model

Sharing will be implemented through two mechanisms.

## Membership Invitation

Users can be invited to a box.

Invitations may grant roles such as:

-   VIEWER
-   CONTRIBUTOR
-   EDITOR

After accepting the invitation, a membership record is created.

------------------------------------------------------------------------

## Share / Access Links (Future)

Share links allow temporary or anonymous access.

Examples:

-   temporary download link
-   public share link
-   password protected link
-   expiring link

Share links may later support:

-   expiration
-   password protection
-   usage limits

------------------------------------------------------------------------

# File Operations

Files inside a box support the following operations.

## Read

Allowed for:

-   owner
-   members
-   public users if box is PUBLIC
-   valid share link (future)

## Upload

Allowed for:

-   OWNER
-   CONTRIBUTOR
-   EDITOR

## Modify

Allowed for:

-   OWNER
-   EDITOR

## Delete

Allowed for:

-   OWNER
-   EDITOR

------------------------------------------------------------------------

# Future Features

The system is designed to support future expansion:

-   preview engine
-   markdown editing
-   collaborative editing
-   file versioning
-   storage quotas
-   distributed storage
-   public box discovery
-   activity logging

------------------------------------------------------------------------

# Design Principles

This project follows several design principles.

## Maintainability

Clear separation of responsibilities and modular domain structure.

## Scalability

Storage and file metadata are separated to support future distributed
storage.

## Extensibility

The system is designed to support new features such as collaboration and
versioning.
