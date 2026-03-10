package dev.haomin.filesheep.auth.service.impl

import dev.haomin.filesheep.domain.account.repo.AccountRepo
import dev.haomin.filesheep.domain.auth.EmailAuth
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * UserDetailsService implementation for email and password authentication.
 */
@Service
class EmailPasswordUserDetailsService(
    private val repo: AccountRepo,
): UserDetailsService {

    /**
     * Loads the user details by username (email in this case).
     *
     * @param username the email of the user
     * @return UserDetails object containing user information
     */
    override fun loadUserByUsername(username: String): UserDetails =
        repo.selectByEmail(username)
            ?.let { EmailAuth(it) }
            ?: throw UsernameNotFoundException("User with email $username not found")
}
