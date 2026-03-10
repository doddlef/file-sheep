package dev.haomin.filesheep.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
class RedisConfiguration {

    @Bean
    fun lettuceConnectionFactory(
        @Value($$"${spring.redis.host:localhost}") host: String,
        @Value($$"${spring.redis.port:6379}") port: Int,
    ): LettuceConnectionFactory {
        val redisConfig = RedisStandaloneConfiguration(host, port)

        val lettuceConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(2))
            .shutdownTimeout(Duration.ZERO)
            .build()

        return LettuceConnectionFactory(redisConfig, lettuceConfig)
    }

    @Bean("redisTemplate")
    fun redisTemplate(
        factory: RedisConnectionFactory, objectMapper: ObjectMapper,
    ): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            connectionFactory = factory

            val strSerializer = StringRedisSerializer()
            val jsonSerializer = GenericJacksonJsonRedisSerializer(objectMapper)

            keySerializer = strSerializer
            valueSerializer = jsonSerializer

            hashKeySerializer = strSerializer
            hashValueSerializer = jsonSerializer

            afterPropertiesSet()
        }

    @Bean("stringRedisTemplate")
    fun stringRedisTemplate(factory: RedisConnectionFactory): StringRedisTemplate =
        StringRedisTemplate(factory)
}