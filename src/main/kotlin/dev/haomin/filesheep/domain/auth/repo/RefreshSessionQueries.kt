package dev.haomin.filesheep.domain.auth.repo

import dev.haomin.filesheep.common.id.UUIDGenerator
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Data class for inserting a refresh session into the database.
 *
 * @property id Session id
 * @property accountId Account id owning this session
 * @property token Current refresh token hash
 * @property createdAt Creation time
 * @property updatedAt Last update time
 */
data class RefreshSessionInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val accountId: UUID,
    val token: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

/**
 * Data class for partially updating a refresh session.
 *
 * @property token New current token hash
 * @property prevToken Previous token hash
 * @property lastUsedAt Last refresh usage time
 * @property revokedAt Revocation timestamp
 * @property revokeReason Revocation reason
 * @property updatedAt Last update time
 */
data class RefreshSessionUpdateQuery(
    val token: String? = null,
    val prevToken: String? = null,
    val lastUsedAt: OffsetDateTime? = null,
    val revokedAt: OffsetDateTime? = null,
    val revokeReason: String? = null,
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
