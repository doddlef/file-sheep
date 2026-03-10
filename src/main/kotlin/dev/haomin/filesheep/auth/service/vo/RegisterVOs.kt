package dev.haomin.filesheep.auth.service.vo

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Command for sending verification email during registration.
 */
data class SendVerificationCmd(
    val email: String,
)

/**
 * Command for verifying a registration code.
 */
data class VerifyCodeCmd(
    val attemptId: String,
    val code: String,
)

/**
 * Command for creating account after verification.
 */
data class CompleteRegistrationCmd(
    val attemptId: String,
    val nickname: String,
    val password: String,
)

/**
 * Command for resending verification code.
 */
data class ResendVerificationCmd(
    val attemptId: String,
)

/**
 * Result for verification-email send and resend operations.
 */
data class SendVerificationResult(
    val attemptId: String,
    val expiresInSeconds: Long,
)

/**
 * Result for verify-code operation.
 */
data class VerifyCodeResult(
    val verified: Boolean,
    val verifiedAt: OffsetDateTime?,
)

/**
 * Result for account creation from a verified attempt.
 */
data class CompleteRegistrationResult(
    val accountId: UUID,
    val email: String,
    val nickname: String,
)

/**
 * Redis model for a temporary registration attempt.
 *
 * Stores only code hash, never raw verification code.
 */
data class RegisterAttempt(
    val attemptId: String,
    val email: String,
    val codeHash: String?,
    val verifiedAt: OffsetDateTime? = null,
    val tryCount: Int = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
