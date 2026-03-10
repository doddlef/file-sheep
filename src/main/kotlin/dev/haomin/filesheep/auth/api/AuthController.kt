package dev.haomin.filesheep.auth.api

import dev.haomin.filesheep.auth.service.AuthService
import dev.haomin.filesheep.auth.service.TokenService
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {

}
