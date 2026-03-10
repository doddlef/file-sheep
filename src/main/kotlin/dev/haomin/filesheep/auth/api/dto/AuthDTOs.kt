package dev.haomin.filesheep.auth.api.dto

import java.time.OffsetDateTime

/**
 * Data class representing a login request using email and password.
 *
 * @param email The email address of the user.
 * @param password The password of the user.
 */
data class EmailPasswordLoginRequest(
    val email: String,
    val password: String,
)

/**
 * Data class representing a request to send a verification code to an email address.
 *
 * @param email The email address to which the verification code will be sent.
 */
data class SendVerificationRequest(
    val email: String,
)

/**
 * Data class representing a request to resend a verification code.
 *
 * @param attemptId The unique identifier for the verification attempt.
 */
data class ResendVerificationRequest(
    val attemptId: String,
)

/**
 * Data class representing a request to verify a code.
 *
 * @param attemptId The unique identifier for the verification attempt.
 * @param code The verification code to be verified.
 */
data class VerifyCodeRequest(
    val attemptId: String,
    val code: String,
)

/**
 * Data class representing a registration request.
 *
 * @param attemptId The unique identifier for the verification attempt.
 * @param nickname The desired nickname for the new user.
 * @param password The desired password for the new user.
 */
data class RegisterRequest(
    val attemptId: String,
    val nickname: String,
    val password: String,
)

/**
 * Data class representing response payload for verification request/resend operations.
 *
 * @param attemptId The unique identifier for the verification attempt.
 * @param expiresInSeconds Remaining attempt lifetime in seconds.
 */
data class VerificationAttemptResponse(
    val attemptId: String,
    val expiresInSeconds: Long,
)

/**
 * Data class representing response payload for code confirmation.
 *
 * @param verified Whether the attempt is verified.
 * @param verifiedAt Verification timestamp.
 */
data class VerifyCodeResponse(
    val verified: Boolean,
    val verifiedAt: OffsetDateTime?,
)

/**
 * Data class representing response payload for register completion.
 *
 * @param accountId The created account id.
 * @param email Registered email.
 * @param nickname Registered nickname.
 */
data class RegisterResponse(
    val accountId: String,
    val email: String,
    val nickname: String,
)
