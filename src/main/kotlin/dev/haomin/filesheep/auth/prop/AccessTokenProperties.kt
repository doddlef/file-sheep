package dev.haomin.filesheep.auth.prop

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "filesheep.auth.access")
data class AccessTokenProperties(
    val secret: String,
    val lifetime: Duration = Duration.ofMinutes(30),
)

@ConfigurationProperties(prefix = "filesheep.auth.refresh")
data class RefreshTokenProperties(
    val secret: String,
    val idleLifetime: Duration = Duration.ofDays(7),
    val maxLifetime: Duration = Duration.ofDays(120),
    val graceTime: Duration = Duration.ofSeconds(5),
)
