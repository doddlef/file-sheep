package dev.haomin.filesheep.auth.service.impl

import dev.haomin.filesheep.auth.service.AuthService
import dev.haomin.filesheep.auth.service.vo.EmailPasswordLoginCmd
import dev.haomin.filesheep.common.exception.AppException
import dev.haomin.filesheep.domain.auth.AuthPrincipal
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class AuthServiceImpl(
    private val authManager: AuthenticationManager,
): AuthService {
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(AuthServiceImpl::class.java)
    }

    override fun emailPasswordLogin(cmd: EmailPasswordLoginCmd): AuthPrincipal {
        val (email, password, context) = cmd
        logger.debug("Login attempt for email: {}, request-context: {}", email, context)

        val principal = UsernamePasswordAuthenticationToken(email, password)
            .let { authManager.authenticate(it) }
            .let {
                if (it.principal is AuthPrincipal)
                    it.principal as AuthPrincipal
                else {
                    val className = it.principal
                        ?.let { principal -> principal::class.simpleName }
                        ?: "unknown"
                    logger.error("Authentication principal is not of type AuthPrincipal, actual type: {}", className)
                    throw AppException()
                }
            }
        logger.debug("login successful for id: {}, email: {}", principal.id, principal.email)

        return principal
    }
}
