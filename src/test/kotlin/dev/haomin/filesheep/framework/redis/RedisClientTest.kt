package dev.haomin.filesheep.framework.redis

import java.util.UUID
import java.util.concurrent.TimeUnit
import dev.haomin.filesheep.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
import org.springframework.data.redis.core.StringRedisTemplate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class RedisClientTest {

    @Autowired
    private lateinit var redisClient: RedisClient

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Test
    fun getExpireReturnsNullWhenKeyNotExists(): Unit {
        val key = "test:redis-client:expire-missing:${UUID.randomUUID()}"

        val ttl = redisClient.getExpire(key)

        assertNull(ttl)
    }

    @Test
    fun getExpireReturnsNoExpireWhenKeyHasNoTtl(): Unit {
        val key = "test:redis-client:no-expire:${UUID.randomUUID()}"
        redisClient.set(key, "value")

        val ttl = redisClient.getExpire(key)

        assertNotNull(ttl)
        assertEquals(KEY_NO_EXPIRE, ttl)
    }

    @Test
    fun setObjAndGetObjRoundTrip(): Unit {
        val key = "test:redis-client:obj:${UUID.randomUUID()}"
        val expected = CachedUser(id = UUID.randomUUID().toString(), name = "alice", age = 24)

        redisClient.setObj(key, expected, 60, TimeUnit.SECONDS)
        val actual: CachedUser? = redisClient.getObj(key)

        assertEquals(expected, actual)
    }

    @Test
    fun throwsPipelineExceptionWhenCalledInPipeline(): Unit {
        val key = "test:redis-client:pipeline:${UUID.randomUUID()}"

        assertFailsWith<PipelineException> {
            stringRedisTemplate.executePipelined(
                object : SessionCallback<Any?> {
                    override fun <K : Any, V : Any> execute(operations: RedisOperations<K, V>): Any? {
                        redisClient.increment(key)
                        return null
                    }
                },
            )
        }
    }

    @Test
    fun hasKeyUsesCorrectParameter(): Unit {
        val key = "test:redis-client:has-key:${UUID.randomUUID()}"
        redisClient.set(key, "ok")

        assertTrue(redisClient.hasKey(key))
    }

    data class CachedUser(
        val id: String,
        val name: String,
        val age: Int,
    )
}
