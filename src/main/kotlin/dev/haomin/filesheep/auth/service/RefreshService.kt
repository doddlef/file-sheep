package dev.haomin.filesheep.auth.service

import dev.haomin.filesheep.auth.service.vo.RefreshSessionProfile
import dev.haomin.filesheep.domain.auth.RefreshSession
import java.util.UUID

interface RefreshService {

    /**
     * The method generates a new refresh session for the specified account ID
     * and returns a profile containing the session's unique identifier and the associated token.
     * This allows the client to maintain a valid refresh session for future authentication requests.
     *
     * @param id The unique identifier of the account.
     * @return The newly created refresh session.
     */
    fun createSession(id: UUID): RefreshSessionProfile

    /**
     * Load a refresh session by its session ID and lock it for update.
     *
     * @param sid The unique identifier of the refresh session.
     * @return The refresh session corresponding to the given session ID.
     * @throws dev.haomin.filesheep.auth.exception.AuthAccountNotFoundException if the session is not found.
     */
    fun loadSession(sid: UUID): RefreshSession

    /**
     * Revokes the refresh session identified by the given session ID.
     *
     * @param sid The unique identifier of the refresh session to be revoked.
     */
    fun revokeSession(sid: UUID, reason: String)

    /**
     * Given sid and currentToken of the current refresh session,
     * update the token of the refresh session to a new
     * value, and replace the previous token with the current one.
     * This allows the client to maintain a valid refresh session
     *
     * @param sid The unique identifier of the refresh session to be rotated.
     * @param currentToken The current token (hashed) of the refresh session
     * @return The token of the newly created refresh session.
     */
    fun rotateSession(sid: UUID, currentToken: String): RefreshSessionProfile
}