package dev.haomin.filesheep.auth.service.impl

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import dev.haomin.filesheep.auth.exception.ExpiredAuthenticationException
import dev.haomin.filesheep.auth.exception.InvalidAuthenticationException
import dev.haomin.filesheep.auth.prop.RefreshTokenProperties
import dev.haomin.filesheep.auth.service.RefreshService
import dev.haomin.filesheep.auth.service.vo.RefreshTokenResult
import dev.haomin.filesheep.common.id.UUIDGenerator
import dev.haomin.filesheep.domain.auth.RefreshSession
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionInsertQuery
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionRepo
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionUpdateQuery

/**
 * Refresh-session implementation with DB-backed rotation and replay detection.
 */
@Service
class RefreshServiceImpl(
    private val refreshSessionRepo: RefreshSessionRepo,
    private val refreshTokenProperties: RefreshTokenProperties,
) : RefreshService {

    private val random: SecureRandom = SecureRandom()
    private val pepperBytes: ByteArray = decodeSecret(refreshTokenProperties.secret)

    override fun createSession(accountId: UUID): RefreshTokenResult {
        val sessionId = UUIDGenerator.next()
        val refreshSecret = generateSecret()
        val refreshHash = hashSecret(refreshSecret)
        val now = OffsetDateTime.now()

        refreshSessionRepo.insert(
            RefreshSessionInsertQuery(
                id = sessionId,
                accountId = accountId,
                token = refreshHash,
                createdAt = now,
                updatedAt = now,
            )
        )

        return RefreshTokenResult(
            accountId = accountId,
            refreshToken = buildRefreshToken(sessionId, refreshSecret),
            expiry = computeExpiry(createdAt = now, lastUsedAt = now),
        )
    }

    @Transactional
    override fun rotateSession(refreshToken: String): RefreshTokenResult {
        val parsed = parseRefreshToken(refreshToken)
        val session = refreshSessionRepo.selectByIdForUpdate(parsed.sessionId)
            ?: throw InvalidAuthenticationException("invalid refresh token")
        val now = OffsetDateTime.now()

        if (session.revokedAt != null) {
            throw InvalidAuthenticationException("refresh session has been revoked")
        }

        if (isSessionExpired(session, now)) {
            revokeSessionById(session.id, reason = "session_expired", now = now)
            throw ExpiredAuthenticationException("refresh session has expired")
        }

        val incomingHash = hashSecret(parsed.secret)
        if (constantTimeEquals(incomingHash, session.token)) {
            return rotateSessionToken(session, now)
        }

        if (session.prevToken != null && constantTimeEquals(incomingHash, session.prevToken)) {
            val graceStartAt = session.lastUsedAt ?: session.updatedAt
            val withinGraceWindow = now <= graceStartAt.plus(refreshTokenProperties.graceTime)
            if (withinGraceWindow) {
                return rotateSessionToken(session, now)
            }
        }

        revokeSessionById(session.id, reason = "refresh_token_reuse_detected", now = now)
        throw InvalidAuthenticationException("refresh token reuse detected")
    }

    @Transactional
    override fun revokeSession(refreshToken: String, reason: String): Boolean {
        val parsed = parseRefreshTokenOrNull(refreshToken) ?: return false
        val session = refreshSessionRepo.selectByIdForUpdate(parsed.sessionId) ?: return false

        if (session.revokedAt != null) {
            return false
        }

        val incomingHash = hashSecret(parsed.secret)
        val matchesKnownToken = constantTimeEquals(incomingHash, session.token) ||
            (session.prevToken != null && constantTimeEquals(incomingHash, session.prevToken))
        if (!matchesKnownToken) {
            return false
        }

        revokeSessionById(session.id, reason = reason, now = OffsetDateTime.now())
        return true
    }

    private fun rotateSessionToken(session: RefreshSession, now: OffsetDateTime): RefreshTokenResult {
        val newSecret = generateSecret()
        val newHash = hashSecret(newSecret)

        refreshSessionRepo.updateById(
            id = session.id,
            query = RefreshSessionUpdateQuery(
                token = newHash,
                prevToken = session.token,
                lastUsedAt = now,
                updatedAt = now,
            )
        )

        return RefreshTokenResult(
            accountId = session.accountId,
            refreshToken = buildRefreshToken(session.id, newSecret),
            expiry = computeExpiry(createdAt = session.createdAt, lastUsedAt = now),
        )
    }

    private fun revokeSessionById(id: UUID, reason: String, now: OffsetDateTime): Unit {
        refreshSessionRepo.updateById(
            id = id,
            query = RefreshSessionUpdateQuery(
                revokedAt = now,
                revokeReason = reason,
                updatedAt = now,
            )
        )
    }

    private fun isSessionExpired(session: RefreshSession, now: OffsetDateTime): Boolean {
        val idleBase = session.lastUsedAt ?: session.createdAt
        val idleExpiry = idleBase.plus(refreshTokenProperties.idleLifetime)
        val absoluteExpiry = session.createdAt.plus(refreshTokenProperties.maxLifetime)

        return now >= idleExpiry || now >= absoluteExpiry
    }

    private fun computeExpiry(
        createdAt: OffsetDateTime,
        lastUsedAt: OffsetDateTime,
    ): OffsetDateTime {
        val idleExpiry = lastUsedAt.plus(refreshTokenProperties.idleLifetime)
        val absoluteExpiry = createdAt.plus(refreshTokenProperties.maxLifetime)
        return if (idleExpiry <= absoluteExpiry) idleExpiry else absoluteExpiry
    }

    private fun generateSecret(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashSecret(secret: String): String {
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(SecretKeySpec(pepperBytes, HMAC_SHA_256))
        val digest = mac.doFinal(secret.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun buildRefreshToken(sessionId: UUID, secret: String): String =
        "$sessionId.$secret"

    private fun parseRefreshToken(token: String): ParsedRefreshToken =
        parseRefreshTokenOrNull(token) ?: throw InvalidAuthenticationException("invalid refresh token format")

    private fun parseRefreshTokenOrNull(token: String): ParsedRefreshToken? {
        val dotIndex = token.indexOf('.')
        if (dotIndex <= 0 || dotIndex == token.lastIndex) {
            return null
        }

        val sessionId = runCatching { UUID.fromString(token.substring(0, dotIndex)) }.getOrNull() ?: return null
        val secret = token.substring(dotIndex + 1)
        if (secret.isBlank()) {
            return null
        }

        return ParsedRefreshToken(sessionId = sessionId, secret = secret)
    }

    private fun decodeSecret(secret: String): ByteArray =
        runCatching { Base64.getDecoder().decode(secret) }
            .getOrElse { secret.toByteArray(StandardCharsets.UTF_8) }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(
            left.toByteArray(StandardCharsets.UTF_8),
            right.toByteArray(StandardCharsets.UTF_8),
        )

    private data class ParsedRefreshToken(
        val sessionId: UUID,
        val secret: String,
    )

    private companion object {
        const val HMAC_SHA_256: String = "HmacSHA256"
    }
}
