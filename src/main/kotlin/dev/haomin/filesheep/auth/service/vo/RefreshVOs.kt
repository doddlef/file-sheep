package dev.haomin.filesheep.auth.service.vo

import java.util.UUID

/**
 * Data class representing the profile information returned when refreshing a session.
 *
 * @property id The unique identifier of the refresh session.
 * @property token The hashed refresh token.
 */
data class RefreshSessionProfile(
    val id: UUID,
    val token: String,
)