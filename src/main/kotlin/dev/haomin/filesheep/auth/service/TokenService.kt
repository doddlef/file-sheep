package dev.haomin.filesheep.auth.service

import java.util.UUID

import org.springframework.http.ResponseCookie

import dev.haomin.filesheep.auth.exception.AuthException
import dev.haomin.filesheep.auth.service.vo.LoginResult
import dev.haomin.filesheep.auth.service.vo.RefreshResult
import dev.haomin.filesheep.domain.auth.TokenAuth

interface TokenService {

    /**
     * Generate a new token pair for the user with the given ID.
     *
     * @param id The UUID of the user.
     * @return A [LoginResult] containing the token pair, user info, and refresh cookie.
     */
    fun login(id: UUID): LoginResult

    /**
     * Refresh the access token using the provided refresh token.
     *
     * @param refreshToken The refresh token.
     * @return A [RefreshResult] containing the new access token and possibly a new refresh token.
     * @throws [AuthException] if the refresh token is invalid or expired.
     */
    fun refresh(refreshToken: String): RefreshResult

    /**
     * Parse the access token and extract the authentication principal.
     *
     * @param accessToken The access token.
     * @return An [TokenAuth] representing the authenticated user.
     * @throws [AuthException] if the access token is invalid or expired.
     */
    fun parseAccessToken(accessToken: String): TokenAuth

    /**
     * Invalidate the provided refresh token, effectively logging the user out.
     *
     * @param refreshToken The refresh token to invalidate.
     */
    fun logout(refreshToken: String): ResponseCookie
}
