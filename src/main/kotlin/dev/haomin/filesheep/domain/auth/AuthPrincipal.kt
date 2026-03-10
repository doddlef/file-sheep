package dev.haomin.filesheep.domain.auth

import dev.haomin.filesheep.domain.account.AccountStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

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