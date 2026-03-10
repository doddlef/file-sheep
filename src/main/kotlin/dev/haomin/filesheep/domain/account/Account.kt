package dev.haomin.filesheep.domain.account

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Account status enum representing the state of a user account.
 *
 * - ACTIVE: User is active and can log in
 * - INACTIVE: User is inactive and cannot log in, may be archived
 * - BANNED: User is banned and cannot log in
 */
enum class AccountStatus {
    ACTIVE,
    INACTIVE,
    BANNED;

    companion object {
        /**
         * Convert a string to AccountStatus.
         *
         * @param value The string value to convert
         * @return The corresponding AccountStatus
         * @throws IllegalArgumentException if the value doesn't match any status
         */
        fun fromString(value: String): AccountStatus =
            entries.find { it.name.uppercase() == value.uppercase() }
                ?: throw IllegalArgumentException("Invalid AccountStatus: $value")
    }
}

/**
 * Represents a registered account in the system.
 *
 * Each user owns a personal cloud drive.
 *
 * @property id Unique identifier (UUID)
 * @property email User's primary email (unique, citext)
 * @property nick User's nickname
 * @property password Password hash (never store plain text)
 * @property status Current account status
 * @property avatar Optional avatar URL
 * @property createdAt When the account was created
 * @property updatedAt When the account was last updated
 */
data class Account(
    val id: UUID,
    val email: String,
    val nick: String,
    val password: String,
    val status: AccountStatus,
    val avatar: String? = null,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
