package dev.haomin.filesheep.common.exception

import dev.haomin.filesheep.common.response.ResponseCode
import org.springframework.http.HttpStatus

/**
 * The base exception for all custom exceptions in the Secure Share application.
 *
 * @param code The response code associated with the exception.
 * @param message The detail message for the exception.
 * @param payload Additional data related to the exception.
 * @param status The HTTP status code for the exception.
 * @param cause The underlying cause of the exception.
 */
open class AppException(
    val code: ResponseCode = ResponseCode.FAILURE,
    message: String = code.description,
    val payload: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
): RuntimeException(message, cause) {
    val status: HttpStatus get() = code.status
}

/**
 * Exception thrown when an invalid parameter is encountered.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class InvalidParamException(
    message: String = ResponseCode.INVALID_PARAM.description,
    cause: Throwable? = null,
): AppException(code = ResponseCode.INVALID_PARAM, message = message, cause = cause)

/**
 * Exception thrown when a requested resource is not found.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class ConflictException(
    message: String = ResponseCode.CONFLICT.description,
    cause: Throwable? = null,
): AppException(code = ResponseCode.CONFLICT, message = message, cause = cause)

/**
 * Exception thrown when a database operation fails.
 *
 * @param message The detail message for the exception.
 * @param cause The underlying cause of the exception.
 */
class DatabaseOperationException(
    message: String = "Database operation failed",
    cause: Throwable? = null,
): AppException(code = ResponseCode.FAILURE, message = message, cause = cause)