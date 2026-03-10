package dev.haomin.filesheep.auth.service.impl

import dev.haomin.filesheep.auth.REFRESH_TOKEN_COOKIE
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys

import dev.haomin.filesheep.auth.exception.ExpiredAuthenticationException
import dev.haomin.filesheep.auth.prop.AccessTokenProperties
import dev.haomin.filesheep.auth.prop.RefreshTokenProperties
import dev.haomin.filesheep.auth.service.RefreshService
import dev.haomin.filesheep.auth.service.vo.RefreshTokenResult
import dev.haomin.filesheep.domain.account.Account
import dev.haomin.filesheep.domain.account.AccountStatus
import dev.haomin.filesheep.domain.account.repo.AccountRepo
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TokenServiceImplTest {

    private val accountRepo: AccountRepo = mock(AccountRepo::class.java)
    private val refreshService: RefreshService = mock(RefreshService::class.java)
    private val accessProps = AccessTokenProperties(
        secret = "fLqXLI7VVFXexFdAiJTSLhhSjuUHG3VyGNuppEwFlndNzMZihjyyNot85YRwSGWTYN6N32zRozGeb4Qd+ZWURQ==",
        lifetime = Duration.ofMinutes(15),
    )
    private val refreshProps = RefreshTokenProperties(
        secret = "fLqXLI7VVFXexFdAiJTSLhhSjuUHG3VyGNuppEwFlndNzMZihjyyNot85YRwSGWTYN6N32zRozGeb4Qd+ZWURQ==",
        idleLifetime = Duration.ofDays(7),
        maxLifetime = Duration.ofDays(30),
        graceTime = Duration.ofSeconds(5),
    )
    private val service = TokenServiceImpl(
        accountRepo = accountRepo,
        refreshService = refreshService,
        accessTokenProperties = accessProps,
        refreshTokenProperties = refreshProps,
    )

    @Test
    fun loginReturnsAccessAndRefreshTokenAndCookie() {
        val account = sampleAccount()
        val refreshToken = "${UUID.randomUUID()}.refresh-secret"
        `when`(accountRepo.selectById(account.id)).thenReturn(account)
        `when`(refreshService.createSession(account.id)).thenReturn(
            RefreshTokenResult(
                accountId = account.id,
                refreshToken = refreshToken,
                expiry = OffsetDateTime.now().plusDays(7),
            )
        )

        val result = service.login(account.id)

        assertEquals(account.id, result.account.id)
        assertNotNull(result.tokenPair.access.token)
        assertEquals(refreshToken, result.tokenPair.refresh.token)
        assertEquals(REFRESH_TOKEN_COOKIE, result.refreshCookie.name)
        assertEquals("/auth", result.refreshCookie.path)
        assertTrue(result.refreshCookie.isHttpOnly)
        assertTrue(result.refreshCookie.isSecure)
        assertEquals("Lax", result.refreshCookie.sameSite)
    }

    @Test
    fun refreshReturnsRotatedRefreshTokenAndNewAccessToken() {
        val account = sampleAccount()
        val incomingRefresh = "${UUID.randomUUID()}.old-secret"
        val rotatedRefresh = "${UUID.randomUUID()}.new-secret"
        `when`(refreshService.rotateSession(incomingRefresh)).thenReturn(
            RefreshTokenResult(
                accountId = account.id,
                refreshToken = rotatedRefresh,
                expiry = OffsetDateTime.now().plusDays(7),
            )
        )
        `when`(accountRepo.selectById(account.id)).thenReturn(account)

        val result = service.refresh(incomingRefresh)

        assertEquals(rotatedRefresh, result.tokenPair.refresh.token)
        assertNotNull(result.tokenPair.access.token)
        assertEquals(REFRESH_TOKEN_COOKIE, result.refreshCookie.name)
        verify(refreshService, times(1)).rotateSession(incomingRefresh)
    }

    @Test
    fun parseAccessTokenReturnsTokenAuth() {
        val account = sampleAccount()
        val loginRefresh = "${UUID.randomUUID()}.refresh"
        `when`(accountRepo.selectById(account.id)).thenReturn(account)
        `when`(refreshService.createSession(account.id)).thenReturn(
            RefreshTokenResult(account.id, loginRefresh, OffsetDateTime.now().plusDays(7))
        )
        val loginResult = service.login(account.id)

        val auth = service.parseAccessToken(loginResult.tokenPair.access.token)

        assertEquals(account.id, auth.id)
        assertEquals(account.email, auth.email)
        assertEquals(account.status, auth.status)
    }

    @Test
    fun parseAccessTokenThrowsExpiredAuthenticationForExpiredJwt() {
        val expired = buildExpiredToken()

        assertThrows<ExpiredAuthenticationException> {
            service.parseAccessToken(expired)
        }
    }

    @Test
    fun logoutRevokesSessionAndReturnsClearingCookie() {
        val refreshToken = "${UUID.randomUUID()}.refresh"
        `when`(refreshService.revokeSession(refreshToken, "logout")).thenReturn(true)

        val cookie = service.logout(refreshToken)

        verify(refreshService, times(1)).revokeSession(refreshToken, "logout")
        assertEquals(REFRESH_TOKEN_COOKIE, cookie.name)
        assertEquals("", cookie.value)
        assertTrue(cookie.maxAge.isZero)
    }

    private fun buildExpiredToken(): String {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessProps.secret))
        val now = OffsetDateTime.now()
        return Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("email", "expired@example.com")
            .claim("status", AccountStatus.ACTIVE.name)
            .issuedAt(Date.from(now.minusMinutes(20).toInstant()))
            .expiration(Date.from(now.minusMinutes(10).toInstant()))
            .signWith(key)
            .compact()
    }

    private fun sampleAccount(): Account =
        Account(
            id = UUID.randomUUID(),
            email = "alice@example.com",
            nick = "alice",
            password = "password-hash",
            status = AccountStatus.ACTIVE,
            avatar = null,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now().minusDays(1),
        )
}
