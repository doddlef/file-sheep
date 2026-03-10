package dev.haomin.filesheep.auth.api

import dev.haomin.filesheep.auth.REFRESH_TOKEN_COOKIE
import dev.haomin.filesheep.auth.api.dto.*
import dev.haomin.filesheep.auth.service.AuthService
import dev.haomin.filesheep.auth.service.RegisterService
import dev.haomin.filesheep.auth.service.TokenService
import dev.haomin.filesheep.auth.service.vo.CompleteRegistrationCmd
import dev.haomin.filesheep.auth.service.vo.EmailPasswordLoginCmd
import dev.haomin.filesheep.auth.service.vo.ResendVerificationCmd
import dev.haomin.filesheep.auth.service.vo.SendVerificationCmd
import dev.haomin.filesheep.auth.service.vo.VerifyCodeCmd
import dev.haomin.filesheep.common.response.ApiResponse
import dev.haomin.filesheep.common.web.RequestContext
import dev.haomin.filesheep.domain.account.AccountProfile
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
    private val registerService: RegisterService,
) {

    @PostMapping("/")
    fun emailPasswordLogin(
        @RequestBody dto: EmailPasswordLoginRequest, request: HttpServletRequest,
    ): ResponseEntity<ApiResponse> {
        val principal = EmailPasswordLoginCmd(
            email = dto.email,
            password = dto.password,
            context = RequestContext.from(request)
        )
            .let { authService.emailPasswordLogin(it) }

        val (tokenPair, user, refreshCookie) = tokenService.login(principal.id)
        val body = ApiResponse.success()
            .with("account", AccountProfile.from(user))
            .with(
                "tokens", mapOf(
                    "access_token" to tokenPair.access.token,
                    "refresh_token" to tokenPair.refresh.token,
                    "access_expires" to tokenPair.access.expiry,
                    "refresh_expires" to tokenPair.refresh.expiry,
                )
            )
            .build()

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(body)
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(REFRESH_TOKEN_COOKIE) refreshToken: String,
    ): ResponseEntity<ApiResponse> {
        val (tokenPair, refreshCookie) = tokenService.refresh(refreshToken)
        val body = ApiResponse.success()
            .with(
                "tokens", mapOf(
                    "access_token" to tokenPair.access.token,
                    "refresh_token" to tokenPair.refresh.token,
                    "access_expires" to tokenPair.access.expiry,
                    "refresh_expires" to tokenPair.refresh.expiry,
                )
            )
            .build()

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(body)
    }

    @PostMapping("/logout")
    fun post(@CookieValue(REFRESH_TOKEN_COOKIE) refreshToken: String): ResponseEntity<ApiResponse> {
        val cleanCookie = tokenService.logout(refreshToken)

        return ApiResponse.success("logged out successfully.")
            .build()
            .let { body ->
                ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                    .body(body)
            }
    }

    @PostMapping("/verify/request/")
    fun sendVerification(@RequestBody dto: SendVerificationRequest): ResponseEntity<ApiResponse> {
        val result = registerService.sendVerificationEmail(
            SendVerificationCmd(email = dto.email),
        )
        val response = VerificationAttemptResponse(
            attemptId = result.attemptId,
            expiresInSeconds = result.expiresInSeconds,
        )

        return ApiResponse.success("verification email sent")
            .with("verification", response)
            .build()
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/verify/resend/")
    fun resendVerification(@RequestBody dto: ResendVerificationRequest): ResponseEntity<ApiResponse> {
        val result = registerService.resendVerification(
            ResendVerificationCmd(attemptId = dto.attemptId),
        )
        val response = VerificationAttemptResponse(
            attemptId = result.attemptId,
            expiresInSeconds = result.expiresInSeconds,
        )

        return ApiResponse.success("verification email resent")
            .with("verification", response)
            .build()
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/verify/confirm/")
    fun confirmVerification(@RequestBody dto: VerifyCodeRequest): ResponseEntity<ApiResponse> {
        val result = registerService.verifyCode(
            VerifyCodeCmd(
                attemptId = dto.attemptId,
                code = dto.code,
            ),
        )
        val response = VerifyCodeResponse(
            verified = result.verified,
            verifiedAt = result.verifiedAt,
        )

        return ApiResponse.success("verification confirmed")
            .with("verification", response)
            .build()
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/register/")
    fun completeRegistration(@RequestBody dto: RegisterRequest): ResponseEntity<ApiResponse> {
        val result = registerService.completeRegistration(
            CompleteRegistrationCmd(
                attemptId = dto.attemptId,
                nickname = dto.nickname,
                password = dto.password,
            ),
        )
        val response = RegisterResponse(
            accountId = result.accountId.toString(),
            email = result.email,
            nickname = result.nickname,
        )

        return ApiResponse.success("registration completed")
            .with("account", response)
            .build()
            .let { ResponseEntity.ok(it) }
    }
}
