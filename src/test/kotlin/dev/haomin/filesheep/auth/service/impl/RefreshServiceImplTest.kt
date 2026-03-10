package dev.haomin.filesheep.auth.service.impl

import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import dev.haomin.filesheep.auth.exception.ExpiredAuthenticationException
import dev.haomin.filesheep.auth.exception.InvalidAuthenticationException
import dev.haomin.filesheep.auth.prop.RefreshTokenProperties
import dev.haomin.filesheep.domain.auth.RefreshSession
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionInsertQuery
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionRepo
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionUpdateQuery
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RefreshServiceImplTest {

    private val repo = StubRefreshSessionRepo()
    private val props = RefreshTokenProperties(
        secret = "fLqXLI7VVFXexFdAiJTSLhhSjuUHG3VyGNuppEwFlndNzMZihjyyNot85YRwSGWTYN6N32zRozGeb4Qd+ZWURQ==",
        idleLifetime = Duration.ofDays(7),
        maxLifetime = Duration.ofDays(30),
        graceTime = Duration.ofSeconds(5),
    )
    private val service = RefreshServiceImpl(
        refreshSessionRepo = repo,
        refreshTokenProperties = props,
    )

    @Test
    fun createSessionStoresHashedTokenAndReturnsRawToken() {
        val accountId = UUID.randomUUID()

        val result = service.createSession(accountId)

        val insertQuery = repo.insertCalls.lastOrNull()
        assertNotNull(insertQuery)
        val parts = result.refreshToken.split('.', limit = 2)
        assertEquals(2, parts.size)
        assertEquals(insertQuery.id.toString(), parts[0])
        assertEquals(accountId, insertQuery.accountId)
        assertNotEquals(parts[1], insertQuery.token)
        assertTrue(result.expiry.isAfter(OffsetDateTime.now().plusDays(6)))
    }

    @Test
    fun rotateSessionUpdatesTokenAndPrevTokenWhenCurrentTokenMatches() {
        val accountId = UUID.randomUUID()
        val inserted = service.createSession(accountId)
        val oldParts = inserted.refreshToken.split('.', limit = 2)
        val sessionId = UUID.fromString(oldParts[0])
        val currentHash = repo.insertCalls.lastOrNull()?.token
        assertNotNull(currentHash)

        val session = RefreshSession(
            id = sessionId,
            accountId = accountId,
            token = currentHash,
            prevToken = null,
            lastUsedAt = OffsetDateTime.now().minusMinutes(1),
            revokedAt = null,
            revokeReason = null,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now().minusMinutes(1),
        )
        repo.sessions[sessionId] = session

        val rotated = service.rotateSession(inserted.refreshToken)

        val updatedQueries = repo.updateCalls
        assertEquals(1, updatedQueries.size)
        val (updatedId, update) = updatedQueries.first()
        assertEquals(sessionId, updatedId)
        assertEquals(currentHash, update.prevToken)
        assertNotNull(update.token)
        assertNotEquals(currentHash, update.token)
        assertNotNull(update.lastUsedAt)

        val rotatedParts = rotated.refreshToken.split('.', limit = 2)
        assertEquals(sessionId.toString(), rotatedParts[0])
        assertNotEquals(oldParts[1], rotatedParts[1])
    }

    @Test
    fun rotateSessionRevokesAndThrowsWhenTokenReuseDetected() {
        val accountId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = RefreshSession(
            id = sessionId,
            accountId = accountId,
            token = "current-hash",
            prevToken = "prev-hash",
            lastUsedAt = OffsetDateTime.now().minusMinutes(10),
            revokedAt = null,
            revokeReason = null,
            createdAt = OffsetDateTime.now().minusDays(1),
            updatedAt = OffsetDateTime.now().minusMinutes(10),
        )
        repo.sessions[sessionId] = session
        val raw = "$sessionId.reused-secret"

        val exception = assertThrows<InvalidAuthenticationException> {
            service.rotateSession(raw)
        }

        assertEquals("refresh token reuse detected", exception.message)
        val updatedQueries = repo.updateCalls
        assertEquals(1, updatedQueries.size)
        val (updatedId, update) = updatedQueries.first()
        assertEquals(sessionId, updatedId)
        assertEquals("refresh_token_reuse_detected", update.revokeReason)
        assertNotNull(update.revokedAt)
    }

    @Test
    fun rotateSessionRevokesAndThrowsWhenSessionExpired() {
        val accountId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = RefreshSession(
            id = sessionId,
            accountId = accountId,
            token = "current-hash",
            prevToken = null,
            lastUsedAt = OffsetDateTime.now().minusDays(10),
            revokedAt = null,
            revokeReason = null,
            createdAt = OffsetDateTime.now().minusDays(40),
            updatedAt = OffsetDateTime.now().minusDays(10),
        )
        repo.sessions[sessionId] = session

        assertThrows<ExpiredAuthenticationException> {
            service.rotateSession("$sessionId.some-secret")
        }

        val updatedQueries = repo.updateCalls
        assertEquals(1, updatedQueries.size)
        val (updatedId, update) = updatedQueries.first()
        assertEquals(sessionId, updatedId)
        assertEquals("session_expired", update.revokeReason)
    }

    @Test
    fun revokeSessionReturnsFalseForInvalidTokenFormat() {
        val revoked = service.revokeSession("invalid-format", "logout")
        assertFalse(revoked)
    }

    private class StubRefreshSessionRepo : RefreshSessionRepo {
        val sessions: MutableMap<UUID, RefreshSession> = linkedMapOf()
        val insertCalls: MutableList<RefreshSessionInsertQuery> = mutableListOf()
        val updateCalls: MutableList<Pair<UUID, RefreshSessionUpdateQuery>> = mutableListOf()

        override fun selectByIdForUpdate(id: UUID): RefreshSession? =
            sessions[id]

        override fun insert(query: RefreshSessionInsertQuery): Int {
            insertCalls.add(query)
            sessions[query.id] = RefreshSession(
                id = query.id,
                accountId = query.accountId,
                token = query.token,
                prevToken = null,
                lastUsedAt = null,
                revokedAt = null,
                revokeReason = null,
                createdAt = query.createdAt,
                updatedAt = query.updatedAt,
            )
            return 1
        }

        override fun updateById(id: UUID, query: RefreshSessionUpdateQuery): Int {
            updateCalls.add(id to query)
            val existing = sessions[id] ?: return 0
            sessions[id] = existing.copy(
                token = query.token ?: existing.token,
                prevToken = query.prevToken ?: existing.prevToken,
                lastUsedAt = query.lastUsedAt ?: existing.lastUsedAt,
                revokedAt = query.revokedAt ?: existing.revokedAt,
                revokeReason = query.revokeReason ?: existing.revokeReason,
                updatedAt = query.updatedAt,
            )
            return 1
        }
    }
}
