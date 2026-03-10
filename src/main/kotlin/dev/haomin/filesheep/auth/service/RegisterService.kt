package dev.haomin.filesheep.auth.service

import dev.haomin.filesheep.auth.service.vo.CompleteRegistrationCmd
import dev.haomin.filesheep.auth.service.vo.CompleteRegistrationResult
import dev.haomin.filesheep.auth.service.vo.ResendVerificationCmd
import dev.haomin.filesheep.auth.service.vo.SendVerificationCmd
import dev.haomin.filesheep.auth.service.vo.SendVerificationResult
import dev.haomin.filesheep.auth.service.vo.VerifyCodeCmd
import dev.haomin.filesheep.auth.service.vo.VerifyCodeResult

/**
 * Service interface for handling user registration workflows.
 *
 * This service provides methods for user registration, including sending
 * verification emails, verifying registration codes, completing registration
 * after verification, and resending verification codes for the registration process.
 */
interface RegisterService {

    /**
     * Creates a registration attempt and sends a verification email.
     *
     * @param cmd Command with user email
     * @return Attempt id and attempt expiry information
     */
    fun sendVerificationEmail(cmd: SendVerificationCmd): SendVerificationResult

    /**
     * Verifies the code for a registration attempt.
     *
     * @param cmd Command with attempt id and submitted numeric code
     * @return Verification status
     */
    fun verifyCode(cmd: VerifyCodeCmd): VerifyCodeResult

    /**
     * Completes account creation after email verification succeeded.
     *
     * @param cmd Command containing attempt id, nickname and password
     * @return Created account details
     */
    fun completeRegistration(cmd: CompleteRegistrationCmd): CompleteRegistrationResult

    /**
     * Resends a new code for an existing registration attempt.
     *
     * @param cmd Command with attempt id
     * @return Attempt id and updated attempt expiry information
     */
    fun resendVerification(cmd: ResendVerificationCmd): SendVerificationResult
}
