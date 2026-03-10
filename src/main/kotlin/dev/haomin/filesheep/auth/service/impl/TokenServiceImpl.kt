package dev.haomin.filesheep.auth.service.impl

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

import javax.crypto.SecretKey

import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service

import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys

import dev.haomin.filesheep.auth.exception.*
import dev.haomin.filesheep.auth.prop.*
import dev.haomin.filesheep.auth.REFRESH_TOKEN_COOKIE
import dev.haomin.filesheep.auth.service.*
import dev.haomin.filesheep.auth.service.vo.*
import dev.haomin.filesheep.domain.account.Account
import dev.haomin.filesheep.domain.account.AccountStatus
import dev.haomin.filesheep.domain.account.repo.AccountRepo
import dev.haomin.filesheep.domain.auth.*

/**
 * Default token service implementation.
 *
 * Handles login/refresh/logout flows and access-token parsing.
 */
@Service
class TokenServiceImpl(
    private val accountRepo: AccountRepo,
    private val refreshService: RefreshService,
    private val accessTokenProperties: AccessTokenProperties,
    private val refreshTokenProperties: RefreshTokenProperties,
) : TokenService {

    private val signingKey: SecretKey = buildAccessSigningKey(accessTokenProperties.secret)

    override fun login(id: UUID): LoginResult {
        val account = accountRepo.selectById(id) ?: throw AuthAccountNotFoundException()
        val now = OffsetDateTime.now()
        val access = issueAccessToken(account, now)
        val refresh = refreshService.createSession(account.id)

        return LoginResult(
            tokenPair = TokenPair(
                access = access,
                refresh = Token(token = refresh.refreshToken, expiry = refresh.expiry),
            ),
            account = account,
            refreshCookie = buildRefreshCookie(refresh.refreshToken),
        )
    }

    override fun refresh(refreshToken: String): RefreshResult {
        val now = OffsetDateTime.now()
        val rotated = refreshService.rotateSession(refreshToken)
        val account = accountRepo.selectById(rotated.accountId) ?: throw AuthAccountNotFoundException()
        val access = issueAccessToken(account, now)

        return RefreshResult(
            tokenPair = TokenPair(
                access = access,
                refresh = Token(token = rotated.refreshToken, expiry = rotated.expiry),
            ),
            refreshCookie = buildRefreshCookie(rotated.refreshToken),
        )
    }

    override fun parseAccessToken(accessToken: String): TokenAuth =
        try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(accessToken)
                .payload

            val id = runCatching { UUID.fromString(claims.subject) }.getOrNull()
                ?: throw InvalidAuthenticationException("invalid access token subject")
            val email = claims[ACCESS_CLAIM_EMAIL] as? String
                ?: throw InvalidAuthenticationException("missing email claim")
            val statusText = claims[ACCESS_CLAIM_STATUS] as? String
                ?: throw InvalidAuthenticationException("missing status claim")

            TokenAuth(
                id = id,
                email = email,
                status = AccountStatus.fromString(statusText),
            )
        } catch (_: ExpiredJwtException) {
            throw ExpiredAuthenticationException("access token has expired")
        } catch (_: JwtException) {
            throw InvalidAuthenticationException("invalid access token")
        } catch (_: IllegalArgumentException) {
            throw InvalidAuthenticationException("invalid access token")
        }

    override fun logout(refreshToken: String): ResponseCookie {
        refreshService.revokeSession(refreshToken, reason = "logout")
        return buildRefreshClearCookie()
    }

    private fun issueAccessToken(account: Account, now: OffsetDateTime): Token {
        val expiry = now.plus(accessTokenProperties.lifetime)
        val accessToken = Jwts.builder()
            .subject(account.id.toString())
            .claim(ACCESS_CLAIM_EMAIL, account.email)
            .claim(ACCESS_CLAIM_STATUS, account.status.name)
            .issuedAt(Date.from(now.toInstant()))
            .expiration(Date.from(expiry.toInstant()))
            .signWith(signingKey)
            .compact()

        return Token(
            token = accessToken,
            expiry = expiry,
        )
    }

    private fun buildRefreshCookie(refreshToken: String): ResponseCookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(true)
            .path(REFRESH_COOKIE_PATH)
            .sameSite(REFRESH_COOKIE_SAME_SITE)
            .maxAge(refreshTokenProperties.maxLifetime)
            .build()

    private fun buildRefreshClearCookie(): ResponseCookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(true)
            .path(REFRESH_COOKIE_PATH)
            .sameSite(REFRESH_COOKIE_SAME_SITE)
            .maxAge(0)
            .build()

    private fun buildAccessSigningKey(secret: String): SecretKey {
        val keyBytes = runCatching { Decoders.BASE64.decode(secret) }
            .getOrElse { secret.toByteArray(StandardCharsets.UTF_8) }
        return Keys.hmacShaKeyFor(keyBytes)
    }

    private companion object {
        const val ACCESS_CLAIM_EMAIL: String = "email"
        const val ACCESS_CLAIM_STATUS: String = "status"
        const val REFRESH_COOKIE_NAME: String = REFRESH_TOKEN_COOKIE
        const val REFRESH_COOKIE_PATH: String = "/auth"
        const val REFRESH_COOKIE_SAME_SITE: String = "Lax"
    }
}
