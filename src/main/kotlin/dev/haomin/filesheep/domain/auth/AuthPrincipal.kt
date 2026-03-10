package dev.haomin.filesheep.domain.auth

import dev.haomin.filesheep.domain.account.AccountStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

/**
 * Represents an authenticated principal in the system.
 *
 * This interface extends the Spring Security `UserDetails` interface
 * and adds additional properties specific to the application's authentication needs.
 *
 * @property id The unique identifier (UUID) of the principal.
 * @property email The email address of the principal, serving as their username.
 * @property status The current account status of the principal, determining their access level.
 *
 * In addition to the properties, this interface provides custom implementations of the
 * `UserDetails` methods to align with the authentication and authorization requirements.
 * - `getUsername`: Returns the email address as the username.
 * - `getAuthorities`: Provides authorities based on the principal's status,
 *   formatted as `STATUS_{STATUS_NAME}`.
 * - `isAccountNonLocked`: Determines if the account is non-locked,
 *   where accounts with a `BANNED` status are considered locked.
 * - `isAccountNonExpired`: Determines if the account is non-expired,
 *   where accounts with an `INACTIVE` status are considered expired.
 */
interface AuthPrincipal: UserDetails {
    val id: UUID
    val email: String
    val status: AccountStatus

    override fun getUsername(): String =
        email

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf("STATUS_${status.name}").map { SimpleGrantedAuthority(it) }

    override fun isAccountNonLocked(): Boolean =
        status != AccountStatus.BANNED

    override fun isAccountNonExpired(): Boolean =
        status != AccountStatus.INACTIVE
}