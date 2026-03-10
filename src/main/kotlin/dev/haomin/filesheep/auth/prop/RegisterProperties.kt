package dev.haomin.filesheep.auth.prop

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for user registration in the authentication system.
 *
 * These properties define the behavior and constraints for user registration,
 * including how long verification attempts are valid, the characteristics of
 * verification codes, and limitations on retries and resend operations.
 *
 * @property attemptLifetime The duration for which a registration attempt remains valid.
 * @property codeLength The length of the verification code sent to users.
 * @property resendCooldown The duration users must wait before they can request to resend the verification code.
 * @property maxVerifyTries The maximum number of attempts allowed for verifying a registration code.
 */
@ConfigurationProperties(prefix = "filesheep.auth.register")
data class RegisterProperties(
    val attemptLifetime: Duration = Duration.ofMinutes(10),
    val codeLength: Int = 4,
    val resendCooldown: Duration = Duration.ofMinutes(1),
    val maxVerifyTries: Int = 5,
    val codeHashSecret: String = "fLqXLI7VVFXexFdAiJTSLhhSjuUHG3VyGNuppEwFlndNzMZihjyyNot85YRwSGWTYN6N32zRozGeb4Qd+ZWURQ==",
)
