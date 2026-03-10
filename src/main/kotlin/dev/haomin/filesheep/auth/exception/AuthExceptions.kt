package dev.haomin.filesheep.auth.exception

import dev.haomin.filesheep.common.exception.AppException
import dev.haomin.filesheep.common.response.ResponseCode

/**
 * Represents an authentication-related exception within the application.
 *
 * This class serves as a base for exceptions triggered by authentication failures.
 * It extends [AppException] to provide additional context specific to authentication
 * errors, such as failure codes and messages. Developers can subclass this exception
 * to handle more specific authentication-related issues.
 *
 * @param code The response code representing the specific authentication error.
 *             Defaults to [ResponseCode.AUTH_FAILED].
 * @param message A detailed message explaining the authentication issue.
 * @param cause The underlying cause of the exception, if available.
 */
open class AuthException(
    code: ResponseCode = ResponseCode.AUTH_FAILED,
    message: String,
    cause: Throwable? = null,
) : AppException(code = code, message = message, cause = cause)

class AuthAccountNotFoundException(
    message: String = "authenticated account not found",
) : AuthException(message = message)

class InvalidAuthenticationException(
    message: String = "invalid authentication",
) : AuthException(code = ResponseCode.INVALID_AUTH, message = message)

class ExpiredAuthenticationException(
    message: String = "authentication has expired",
) : AuthException(code = ResponseCode.EXPIRED_AUTH, message = message)

class RequiresAuthenticationException(
    message: String = "authentication required",
) : AuthException(code = ResponseCode.REQUIRE_AUTH, message = message)