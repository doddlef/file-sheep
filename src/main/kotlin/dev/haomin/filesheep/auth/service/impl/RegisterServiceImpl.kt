package dev.haomin.filesheep.auth.service.impl

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

import dev.haomin.filesheep.auth.prop.RegisterProperties
import dev.haomin.filesheep.auth.service.RegisterService
import dev.haomin.filesheep.auth.service.vo.*
import dev.haomin.filesheep.common.exception.ConflictException
import dev.haomin.filesheep.common.exception.InvalidParamException
import dev.haomin.filesheep.common.id.UUIDGenerator
import dev.haomin.filesheep.common.utils.ValidatorUtils
import dev.haomin.filesheep.domain.account.repo.AccountInsertQuery
import dev.haomin.filesheep.domain.account.repo.AccountRepo
import dev.haomin.filesheep.framework.redis.KEY_NO_EXPIRE
import dev.haomin.filesheep.framework.redis.RedisClient
import dev.haomin.filesheep.framework.redis.getObj
import org.slf4j.Logger
import java.time.Duration

/**
 * Register service implementation backed by Redis temporary attempts.
 */
@Service
class RegisterServiceImpl(
    private val accountRepo: AccountRepo,
    private val redisClient: RedisClient,
    private val passwordEncoder: PasswordEncoder,
    private val registerProperties: RegisterProperties,
) : RegisterService {

    private val random: SecureRandom = SecureRandom()
    private val hashSecretBytes: ByteArray = decodeSecret(registerProperties.codeHashSecret)

    override fun sendVerificationEmail(cmd: SendVerificationCmd): SendVerificationResult {
        val email = normalizeEmail(cmd.email)
        ValidatorUtils.validateEmail(email)

        if (accountRepo.selectByEmail(email) != null) {
            throw ConflictException("email has already been registered")
        }

        val existingAttemptId = redisClient.get(emailKey(email))
        if (existingAttemptId != null) {
            val existingAttempt = redisClient.getObj<RegisterAttempt>(attemptKey(existingAttemptId))
            if (existingAttempt != null) {
                return SendVerificationResult(
                    attemptId = existingAttempt.attemptId,
                    expiresInSeconds = remainingTtlSeconds(attemptKey(existingAttempt.attemptId)),
                )
            }
            redisClient.delete(emailKey(email))
        }

        val attemptId = UUIDGenerator.next().toString()
        val code = generateNumericCode(registerProperties.codeLength)
        val now = OffsetDateTime.now()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = email,
            codeHash = hashCode(code),
            verifiedAt = null,
            tryCount = 0,
            createdAt = now,
        )
        val ttlSeconds = durationToSeconds(registerProperties.attemptLifetime)

        redisClient.setObj(attemptKey(attemptId), attempt, ttlSeconds, TimeUnit.SECONDS)
        redisClient.set(emailKey(email), attemptId, ttlSeconds, TimeUnit.SECONDS)

        sendEmailCode(email = email, attemptId = attemptId, code = code)

        return SendVerificationResult(
            attemptId = attemptId,
            expiresInSeconds = ttlSeconds,
        )
    }

    override fun verifyCode(cmd: VerifyCodeCmd): VerifyCodeResult {
        validateAttemptIdFormat(cmd.attemptId)
        validateCodeFormat(cmd.code)
        val key = attemptKey(cmd.attemptId)
        val attempt = loadAttempt(cmd.attemptId)

        if (attempt.verifiedAt != null) {
            return VerifyCodeResult(verified = true, verifiedAt = attempt.verifiedAt)
        }
        if (attempt.tryCount >= registerProperties.maxVerifyTries) {
            throw InvalidParamException("verification attempts exceeded")
        }

        val submittedHash = hashCode(cmd.code)
        if (!constantTimeEquals(submittedHash, attempt.codeHash)) {
            val updatedTryCount = attempt.tryCount + 1
            val updatedAttempt = attempt.copy(tryCount = updatedTryCount)
            saveAttemptWithRemainingTtl(key, updatedAttempt)

            if (updatedTryCount >= registerProperties.maxVerifyTries) {
                throw InvalidParamException("verification attempts exceeded")
            }
            throw InvalidParamException("verification code is invalid")
        }

        val verifiedAt = OffsetDateTime.now()
        val verifiedAttempt = attempt.copy(
            codeHash = null,
            verifiedAt = verifiedAt,
        )
        saveAttemptWithRemainingTtl(key, verifiedAttempt)

        return VerifyCodeResult(
            verified = true,
            verifiedAt = verifiedAt,
        )
    }

    override fun completeRegistration(cmd: CompleteRegistrationCmd): CompleteRegistrationResult {
        validateAttemptIdFormat(cmd.attemptId)
        ValidatorUtils.validateNickname(cmd.nickname)
        ValidatorUtils.validatePassword(cmd.password)

        val attempt = loadAttempt(cmd.attemptId)
        if (attempt.verifiedAt == null) {
            throw InvalidParamException("verification is required before registration")
        }

        if (accountRepo.selectByEmail(attempt.email) != null) {
            throw ConflictException("email has already been registered")
        }

        val accountId = UUIDGenerator.next()
        val nickname = cmd.nickname.trim()
        val passwordHash = passwordEncoder.encode(cmd.password)
            ?: throw InvalidParamException("password hash generation failed")

        try {
            accountRepo.insert(
                AccountInsertQuery(
                    id = accountId,
                    email = attempt.email,
                    nick = nickname,
                    password = passwordHash,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException("email has already been registered", e)
        }

        redisClient.delete(attemptKey(cmd.attemptId))
        redisClient.delete(emailKey(attempt.email))
        redisClient.delete(cooldownKey(cmd.attemptId))

        return CompleteRegistrationResult(
            accountId = accountId,
            email = attempt.email,
            nickname = nickname,
        )
    }

    override fun resendVerification(cmd: ResendVerificationCmd): SendVerificationResult {
        validateAttemptIdFormat(cmd.attemptId)
        val attempt = loadAttempt(cmd.attemptId)
        if (attempt.verifiedAt != null) {
            throw InvalidParamException("attempt is already verified")
        }

        val cooldownKey = cooldownKey(cmd.attemptId)
        if (redisClient.hasKey(cooldownKey)) {
            throw InvalidParamException("verification resend is cooling down")
        }

        val code = generateNumericCode(registerProperties.codeLength)
        val attemptKey = attemptKey(cmd.attemptId)
        val updatedAttempt = attempt.copy(
            codeHash = hashCode(code),
            tryCount = 0,
        )
        saveAttemptWithRemainingTtl(attemptKey, updatedAttempt)

        val cooldownSeconds = durationToSeconds(registerProperties.resendCooldown)
        redisClient.set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS)
        sendEmailCode(email = attempt.email, attemptId = attempt.attemptId, code = code)

        return SendVerificationResult(
            attemptId = attempt.attemptId,
            expiresInSeconds = remainingTtlSeconds(attemptKey),
        )
    }

    private fun normalizeEmail(email: String): String =
        email.trim().lowercase()

    private fun validateCodeFormat(code: String) {
        if (code.length != registerProperties.codeLength || !code.all { it.isDigit() }) {
            throw InvalidParamException("verification code format is invalid")
        }
    }

    private fun loadAttempt(attemptId: String): RegisterAttempt =
        redisClient.getObj<RegisterAttempt>(attemptKey(attemptId))
            ?: throw InvalidParamException("invalid or expired registration attempt")

    private fun validateAttemptIdFormat(attemptId: String) {
        runCatching { UUID.fromString(attemptId) }
            .getOrElse { throw InvalidParamException("attemptId is not a valid UUID") }
    }

    private fun saveAttemptWithRemainingTtl(key: String, attempt: RegisterAttempt) {
        val ttl = remainingTtlSeconds(key)
        redisClient.setObj(key, attempt, ttl, TimeUnit.SECONDS)
    }

    private fun remainingTtlSeconds(key: String): Long {
        val ttl = redisClient.getExpire(key, TimeUnit.SECONDS)
            ?: throw InvalidParamException("invalid or expired registration attempt")
        return when {
            ttl == KEY_NO_EXPIRE -> durationToSeconds(registerProperties.attemptLifetime)
            ttl <= 0L -> throw InvalidParamException("invalid or expired registration attempt")
            else -> ttl
        }
    }

    private fun durationToSeconds(duration: Duration): Long {
        val seconds = duration.seconds
        return if (seconds <= 0) 1L else seconds
    }

    private fun generateNumericCode(length: Int): String {
        if (length <= 0) {
            throw InvalidParamException("verification code length must be positive")
        }

        return buildString(length) {
            repeat(length) {
                append(random.nextInt(10))
            }
        }
    }

    private fun hashCode(code: String): String {
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(SecretKeySpec(hashSecretBytes, HMAC_SHA_256))
        val digest = mac.doFinal(code.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun decodeSecret(secret: String): ByteArray =
        runCatching { Base64.getDecoder().decode(secret) }
            .getOrElse { secret.toByteArray(StandardCharsets.UTF_8) }

    private fun constantTimeEquals(left: String, right: String?): Boolean {
        if (right == null) {
            return false
        }
        return MessageDigest.isEqual(
            left.toByteArray(StandardCharsets.UTF_8),
            right.toByteArray(StandardCharsets.UTF_8),
        )
    }

    private fun sendEmailCode(email: String, attemptId: String, code: String) {
        logger.info(
            "Register code issued. attemptId={}, email={}",
            attemptId,
            maskEmail(email),
        )
        // TODO: integrate mail provider for delivery and remove this placeholder.
        if (code.isEmpty()) {
            throw InvalidParamException("verification code generation failed")
        }
    }

    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 1) {
            return "***"
        }

        val head = email.substring(0, 2)
        val domain = email.substring(at)
        return "$head****$domain"
    }

    private fun attemptKey(attemptId: String): String =
        "register:attempt:$attemptId"

    private fun emailKey(email: String): String =
        "register:email:$email"

    private fun cooldownKey(attemptId: String): String =
        "register:cooldown:$attemptId"

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(RegisterServiceImpl::class.java)
        const val HMAC_SHA_256: String = "HmacSHA256"
    }
}
