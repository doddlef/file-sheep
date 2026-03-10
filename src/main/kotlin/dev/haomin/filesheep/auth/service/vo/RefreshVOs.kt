package dev.haomin.filesheep.auth.service.vo

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Result payload for refresh-session token issuing workflows.
 *
 * @property accountId The account owning the session
 * @property refreshToken The raw refresh token in `sessionId.secret` format
 * @property expiry The refresh token expiry timestamp based on idle/absolute lifetime limits
 */
data class RefreshTokenResult(
    val accountId: UUID,
    val refreshToken: String,
    val expiry: OffsetDateTime,
)
