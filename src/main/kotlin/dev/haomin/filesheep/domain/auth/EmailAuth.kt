package dev.haomin.filesheep.domain.auth

import dev.haomin.filesheep.domain.account.Account
import dev.haomin.filesheep.domain.account.AccountStatus
import java.util.UUID

/**
 * Represents an authenticated user with an email address.
 *
 * @property account The account associated with this authentication.
 */
data class EmailAuth(
    val account: Account,
) : AuthPrincipal {
    override val id: UUID = account.id
    override val email: String = account.email
    override val status: AccountStatus = account.status
    override fun getPassword(): String = account.password
}
