package dev.haomin.filesheep.auth.service.impl

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder

import dev.haomin.filesheep.auth.prop.RegisterProperties
import dev.haomin.filesheep.auth.service.RegisterCodeGenerator
import dev.haomin.filesheep.auth.service.vo.CompleteRegistrationCmd
import dev.haomin.filesheep.auth.service.vo.RegisterAttempt
import dev.haomin.filesheep.auth.service.vo.ResendVerificationCmd
import dev.haomin.filesheep.auth.service.vo.SendVerificationCmd
import dev.haomin.filesheep.auth.service.vo.VerifyCodeCmd
import dev.haomin.filesheep.common.exception.InvalidParamException
import dev.haomin.filesheep.domain.account.repo.AccountRepo
import dev.haomin.filesheep.framework.redis.RedisClient
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterServiceImplTest {

    private val CODE = "0000"

    private val accountRepo: AccountRepo = mock(AccountRepo::class.java)
    private val passwordEncoder: PasswordEncoder = mock(PasswordEncoder::class.java)
    private val codeGenerator: RegisterCodeGenerator = mock(RegisterCodeGenerator::class.java)

    private val redisClient = InMemoryRedisClient()

    private val props = RegisterProperties(
        attemptLifetime = Duration.ofMinutes(10),
        codeLength = 4,
        resendCooldown = Duration.ofMinutes(1),
        maxVerifyTries = 5,
        codeHashSecret = "fLqXLI7VVFXexFdAiJTSLhhSjuUHG3VyGNuppEwFlndNzMZihjyyNot85YRwSGWTYN6N32zRozGeb4Qd+ZWURQ==",
    )

    private val service = RegisterServiceImpl(
        accountRepo = accountRepo,
        redisClient = redisClient,
        passwordEncoder = passwordEncoder,
        registerProperties = props,
        registerCodeGenerator = codeGenerator,
    )

    @Test
    fun sendVerificationEmailCreatesAttemptAndStoresEmailMapping() {
        val email = "alice@example.com"
        `when`(accountRepo.selectByEmail(email)).thenReturn(null)
        `when`(codeGenerator.generate(4)).thenReturn(CODE)

        val result = service.sendVerificationEmail(SendVerificationCmd(email))

        val savedAttempt = redisClient.getObj("register:attempt:${result.attemptId}", RegisterAttempt::class.java)
        val savedEmailMap = redisClient.get("register:email:$email")
        assertNotNull(savedAttempt)
        assertEquals(result.attemptId, savedAttempt.attemptId)
        assertEquals(email, savedAttempt.email)
        assertNull(savedAttempt.verifiedAt)
        assertEquals(0, savedAttempt.tryCount)
        assertNotNull(savedAttempt.codeHash)
        assertEquals(result.attemptId, savedEmailMap)
        assertEquals(600L, result.expiresInSeconds)
    }

    @Test
    fun verifyCodeMarksAttemptVerifiedAndClearsCodeHash() {
        val attemptId = UUID.randomUUID().toString()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "alice@example.com",
            codeHash = hashedCode(),
            verifiedAt = null,
            tryCount = 0,
            createdAt = OffsetDateTime.now().minusMinutes(1),
        )
        redisClient.setObj("register:attempt:$attemptId", attempt, 500L, TimeUnit.SECONDS)

        val result = service.verifyCode(VerifyCodeCmd(attemptId = attemptId, code = CODE))

        val updated = redisClient.getObj("register:attempt:$attemptId", RegisterAttempt::class.java)
        assertTrue(result.verified)
        assertNotNull(result.verifiedAt)
        assertNotNull(updated)
        assertNull(updated.codeHash)
        assertNotNull(updated.verifiedAt)
    }

    @Test
    fun verifyCodeWithWrongCodeIncrementsTryCountAndThrows() {
        val attemptId = UUID.randomUUID().toString()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "alice@example.com",
            codeHash = hashedCode(),
            verifiedAt = null,
            tryCount = 2,
            createdAt = OffsetDateTime.now().minusMinutes(1),
        )
        redisClient.setObj("register:attempt:$attemptId", attempt, 500L, TimeUnit.SECONDS)

        assertThrows<InvalidParamException> {
            service.verifyCode(VerifyCodeCmd(attemptId = attemptId, code = "1111"))
        }

        val updated = redisClient.getObj("register:attempt:$attemptId", RegisterAttempt::class.java)
        assertNotNull(updated)
        assertEquals(3, updated.tryCount)
    }

    @Test
    fun completeRegistrationInsertsAccountAndCleansRedisKeys() {
        val attemptId = UUID.randomUUID().toString()
        val email = "alice@example.com"
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = email,
            codeHash = null,
            verifiedAt = OffsetDateTime.now().minusMinutes(1),
            tryCount = 1,
            createdAt = OffsetDateTime.now().minusMinutes(5),
        )
        redisClient.setObj("register:attempt:$attemptId", attempt, 500L, TimeUnit.SECONDS)

        `when`(accountRepo.selectByEmail(email)).thenReturn(null)
        `when`(passwordEncoder.encode("password-123")).thenReturn("hashed-password")

        val result = service.completeRegistration(
            CompleteRegistrationCmd(
                attemptId = attemptId,
                nickname = "Alice",
                password = "password-123",
            ),
        )

        verify(accountRepo, times(1)).selectByEmail(email)
        assertNull(redisClient.get("register:attempt:$attemptId"))
        assertNull(redisClient.get("register:email:$email"))
        assertNull(redisClient.get("register:cooldown:$attemptId"))
        assertEquals(email, result.email)
        assertEquals("Alice", result.nickname)
    }

    @Test
    fun resendVerificationThrowsWhenCooldownExists() {
        val attemptId = UUID.randomUUID().toString()
        val attempt = RegisterAttempt(
            attemptId = attemptId,
            email = "alice@example.com",
            codeHash = hashedCode(),
            verifiedAt = null,
            tryCount = 1,
            createdAt = OffsetDateTime.now().minusMinutes(1),
        )
        redisClient.setObj("register:attempt:$attemptId", attempt, 500L, TimeUnit.SECONDS)
        redisClient.set("register:cooldown:$attemptId", "1", 60L, TimeUnit.SECONDS)

        assertThrows<InvalidParamException> {
            service.resendVerification(ResendVerificationCmd(attemptId = attemptId))
        }
    }

    private fun hashedCode(): String {
        val secretBytes = Base64.getDecoder().decode(props.codeHashSecret)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
        val digest = mac.doFinal(CODE.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private class InMemoryRedisClient : RedisClient(
        mock(StringRedisTemplate::class.java),
        mock(ObjectMapper::class.java),
    ) {
        private val values: MutableMap<String, String> = linkedMapOf()
        private val objects: MutableMap<String, Any> = linkedMapOf()
        private val ttls: MutableMap<String, Long> = linkedMapOf()

        override fun get(key: String): String? =
            values[key]

        override fun set(key: String, value: String, expire: Long?, unit: TimeUnit) {
            values[key] = value
            expire?.let {
                ttls[key] = TimeUnit.SECONDS.convert(it, unit)
            }
        }

        override fun setObj(key: String, value: Any, expire: Long?, unit: TimeUnit) {
            objects[key] = value
            expire?.let {
                ttls[key] = TimeUnit.SECONDS.convert(it, unit)
            }
        }

        override fun <T> getObj(key: String, asClass: Class<T>): T? =
            objects[key]
                ?.takeIf { asClass.isInstance(it) }
                ?.let { asClass.cast(it) }

        override fun hasKey(key: String): Boolean =
            values.containsKey(key) || objects.containsKey(key)

        override fun getExpire(key: String, unit: TimeUnit): Long? {
            if (!hasKey(key)) {
                return null
            }
            val seconds = ttls[key] ?: return -1L
            return unit.convert(seconds, TimeUnit.SECONDS)
        }

        override fun delete(key: String): Boolean {
            val removedValue = values.remove(key)
            val removedObject = objects.remove(key)
            ttls.remove(key)
            return removedValue != null || removedObject != null
        }
    }
}
