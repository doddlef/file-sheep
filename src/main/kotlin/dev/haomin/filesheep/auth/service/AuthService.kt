package dev.haomin.filesheep.auth.service

import dev.haomin.filesheep.auth.service.vo.EmailPasswordLoginCmd
import dev.haomin.filesheep.domain.auth.AuthPrincipal

/**
 * Service interface for handling authentication workflows.
 *
 * Provides methods for authenticating users via email and password.
 */
interface AuthService {
    /**
     * Authenticate a user using email and password.
     *
     * @param cmd Command object containing email and password.
     * @return AuthPrincipal representing the authenticated user.
     */
    fun emailPasswordLogin(cmd: EmailPasswordLoginCmd): AuthPrincipal
}