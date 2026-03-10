package dev.haomin.filesheep.auth.service

import dev.haomin.filesheep.auth.exception.ExpiredAuthenticationException
import dev.haomin.filesheep.auth.exception.InvalidAuthenticationException
import dev.haomin.filesheep.auth.service.vo.LoginResult
import dev.haomin.filesheep.auth.service.vo.RefreshResult
import java.util.UUID

interface TokenService {

    /**
     * Generate a new token pair for the user with the given ID.
     *
     * This will create a new refresh session for the user, generate token pairs, and set the refresh cookie.
     *
     * @param id The UUID of the user.
     * @return A [LoginResult] containing the token pair, user info, and refresh cookie.
     */
    fun login(id: UUID): LoginResult

    /**
     * Refresh the access token using the provided refresh token.
     * This will pase the token and validate the refresh session.
     *
     * If the session is expired due to idle timeout or maximum lifetime exceeded,
     * an [ExpiredAuthenticationException] will be thrown.
     *
     * If the jti does not match the current token but matches the previous one, and within the grace period,
     * will return false.
     *
     * If the jti does not match current or previous token, an [InvalidAuthenticationException] will be thrown,
     * and the session should be revoked, due to reuse.
     *
     * @param refreshToken The refresh token.
     * @return A [RefreshResult] containing the new access token and possibly a new refresh token.
     */
    fun refresh(refreshToken: String): RefreshResult


}