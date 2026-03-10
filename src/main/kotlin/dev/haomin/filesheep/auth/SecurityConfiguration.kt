package dev.haomin.filesheep.auth

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = ["dev.haomin.filesheep.auth.prop"])
class SecurityConfiguration {

}
