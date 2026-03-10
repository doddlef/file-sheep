package dev.haomin.filesheep.auth.service.vo

import dev.haomin.filesheep.domain.account.Account
import dev.haomin.filesheep.domain.auth.TokenPair
import org.springframework.http.ResponseCookie

data class LoginResult(
    val tokenPair: TokenPair,
    val account: Account,
    val refreshCookie: ResponseCookie
)

data class RefreshResult(
    val tokenPair: TokenPair,
    val refreshCookie: ResponseCookie,
)