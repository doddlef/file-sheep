package dev.haomin.filesheep.domain.auth

import dev.haomin.filesheep.domain.account.AccountStatus
import java.util.UUID

/**
 * Represents an authenticated user through token-based authentication.
 *
 * @property id The unique identifier (UUID) of the authenticated user.
 * @property email The email address of the authenticated user, used as the username.
 * @property status The current account status of the user,
 *   determining their access permissions and login eligibility.
 *
 * This implementation of [AuthPrincipal] is specific to scenarios where token-based
 * authentication is used. The `getPassword` method is overridden to return `null`, as
 * password-based authentication is not relevant for this use case.
 */
data class TokenAuth(
    override val id: UUID,
    override val email: String,
    override val status: AccountStatus,
): AuthPrincipal {
    override fun getPassword(): String? = null
}
