package dev.haomin.filesheep.auth.service.vo

import dev.haomin.filesheep.common.web.RequestContext

/**
 * Command object for handling email and password-based login requests.
 *
 * @property email The email address provided by the user for authentication.
 * @property password The plaintext password provided by the user for authentication.
 * @property context The request context containing metadata about the incoming HTTP request,
 *   such as IP address, user-agent, and request ID.
 */
data class EmailPasswordLoginCmd(
    val email: String,
    val password: String,
    val context: RequestContext,
)

