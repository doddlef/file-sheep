package dev.haomin.filesheep.domain.auth

import java.time.OffsetDateTime

data class Token(
    val token: String,
    val expiry: OffsetDateTime,
)

data class TokenPair(
    val access: Token,
    val refresh: Token,
)