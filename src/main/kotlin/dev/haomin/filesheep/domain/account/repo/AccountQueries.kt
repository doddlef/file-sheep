package dev.haomin.filesheep.domain.account.repo

import dev.haomin.filesheep.common.id.UUIDGenerator
import dev.haomin.filesheep.domain.account.AccountStatus
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Data class for inserting a new account into the database.
 *
 * Used as a command/query object for account creation operations.
 *
 * @property id Unique identifier (UUID), defaults to time-based UUID
 * @property email User's primary email (unique, citext)
 * @property nick User's nickname
 * @property password Password hash (never store plain text)
 * @property status Account status, defaults to ACTIVE
 * @property avatar Optional avatar URL
 * @property createdAt When the account was created, defaults to current time
 * @property updatedAt When the account was last updated, defaults to current time
 */
data class AccountInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val email: String,
    val nick: String,
    val password: String,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val avatar: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

/**
 * Data class for updating an existing account in the database.
 *
 * Used as a command/query object for account update operations.
 * Only the provided fields will be updated (partial update).
 *
 * @property nick User's nickname
 * @property password Password hash (never store plain text)
 * @property status Account status
 * @property avatar Optional avatar URL
 * @property updatedAt When the account was last updated, defaults to current time
 */
data class AccountUpdateQuery(
    val nick: String? = null,
    val password: String? = null,
    val status: AccountStatus? = null,
    val avatar: String? = null,
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
