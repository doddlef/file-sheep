package dev.haomin.filesheep.auth.service

import java.util.UUID

import dev.haomin.filesheep.auth.service.vo.RefreshTokenResult

/**
 * Service contract for refresh session lifecycle management.
 */
interface RefreshService {

    /**
     * Creates a new refresh session and issues a refresh token.
     *
     * @param accountId The account id that owns the session
     * @return Issued refresh token payload
     */
    fun createSession(accountId: UUID): RefreshTokenResult

    /**
     * Validates and rotates a refresh token.
     *
     * This operation must detect token reuse and revoke suspicious sessions.
     *
     * @param refreshToken The raw refresh token in `sessionId.secret` format
     * @return Newly issued refresh token payload
     */
    fun rotateSession(refreshToken: String): RefreshTokenResult

    /**
     * Revokes a refresh session represented by the provided token.
     *
     * @param refreshToken The raw refresh token in `sessionId.secret` format
     * @param reason The revocation reason persisted in the session row
     * @return True if a session was revoked, otherwise false
     */
    fun revokeSession(refreshToken: String, reason: String = "logout"): Boolean
}
