package dev.haomin.filesheep.domain.auth

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents a refresh token session for authentication.
 *
 * Supports refresh token rotation and session revocation.
 * Tokens are stored as hashes for security (never store refresh tokens in plain text).
 *
 * @property id Unique identifier (UUID)
 * @property accountId Reference to the account owning this session
 * @property token Hash of the current active refresh token
 * @property prevToken Hash of the last used refresh token (for rotation)
 * @property lastUsedAt When the refresh session was last rotated
 * @property revokedAt When the refresh session was revoked (null if active)
 * @property revokeReason Reason for revocation (only set when revoked_at is set)
 * @property createdAt When the session was created
 * @property updatedAt When the session was last updated
 */
data class RefreshSession(
    val id: UUID,
    val accountId: UUID,
    val token: String,
    val prevToken: String? = null,
    val lastUsedAt: OffsetDateTime? = null,
    val revokedAt: OffsetDateTime? = null,
    val revokeReason: String? = null,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
