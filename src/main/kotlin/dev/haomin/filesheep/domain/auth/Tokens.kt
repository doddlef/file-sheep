package dev.haomin.filesheep.domain.auth

import java.time.OffsetDateTime

data class JwtToken(
    val token: String,
    val expiry: OffsetDateTime,
)

data class TokenPair(
    val access: JwtToken,
    val refresh: JwtToken,
)