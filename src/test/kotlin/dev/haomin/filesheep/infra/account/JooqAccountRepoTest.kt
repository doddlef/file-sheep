package dev.haomin.filesheep.infra.account

import dev.haomin.filesheep.TestcontainersConfiguration
import dev.haomin.filesheep.domain.account.AccountStatus
import dev.haomin.filesheep.domain.account.repo.AccountInsertQuery
import dev.haomin.filesheep.domain.account.repo.AccountRepo
import dev.haomin.filesheep.domain.account.repo.AccountUpdateQuery
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class JooqAccountRepoTest {

    @Autowired
    private lateinit var accountRepo: AccountRepo

    @Autowired
    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setUp(): Unit {
        dsl.execute("truncate table refresh_sessions, accounts cascade")
    }

    @Test
    fun insertAndSelectById(): Unit {
        val id = UUID.randomUUID()
        val createdAt = OffsetDateTime.now().minusDays(1)
        val updatedAt = OffsetDateTime.now().minusDays(1)

        val affected = accountRepo.insert(
            AccountInsertQuery(
                id = id,
                email = "alice@example.com",
                nick = "alice",
                password = "hash-1",
                status = AccountStatus.ACTIVE,
                avatar = "https://cdn.example.com/alice.png",
                createdAt = createdAt,
                updatedAt = updatedAt,
            ),
        )

        assertEquals(1, affected)

        val account = accountRepo.selectById(id)
        assertNotNull(account)
        assertEquals(id, account.id)
        assertEquals("alice@example.com", account.email)
        assertEquals("alice", account.nick)
        assertEquals("hash-1", account.password)
        assertEquals(AccountStatus.ACTIVE, account.status)
        assertEquals("https://cdn.example.com/alice.png", account.avatar)
    }

    @Test
    fun selectByEmailReturnsNullWhenNotFound(): Unit {
        val account = accountRepo.selectByEmail("missing@example.com")
        assertNull(account)
    }

    @Test
    fun updateByIdUpdatesOnlyProvidedFields(): Unit {
        val id = UUID.randomUUID()
        accountRepo.insert(
            AccountInsertQuery(
                id = id,
                email = "bob@example.com",
                nick = "bob",
                password = "hash-old",
                status = AccountStatus.ACTIVE,
                avatar = "https://cdn.example.com/bob.png",
            ),
        )
        val before = accountRepo.selectById(id)
        assertNotNull(before)

        val affected = accountRepo.updateById(
            id = id,
            query = AccountUpdateQuery(
                nick = "bobby",
                password = "hash-new",
                status = AccountStatus.BANNED,
                avatar = null,
                updatedAt = OffsetDateTime.now().plusSeconds(5),
            ),
        )

        assertEquals(1, affected)

        val account = accountRepo.selectById(id)
        assertNotNull(account)
        assertEquals("bobby", account.nick)
        assertEquals("hash-new", account.password)
        assertEquals(AccountStatus.BANNED, account.status)
        assertEquals("https://cdn.example.com/bob.png", account.avatar)
        assertTrue(account.updatedAt.isAfter(before.updatedAt) || account.updatedAt.isEqual(before.updatedAt))
    }

    @Test
    fun updateByIdWithNoMutableFieldsStillUpdatesTimestamp(): Unit {
        val id = UUID.randomUUID()
        accountRepo.insert(
            AccountInsertQuery(
                id = id,
                email = "carol@example.com",
                nick = "carol",
                password = "hash-carol",
                status = AccountStatus.ACTIVE,
            ),
        )
        val before = accountRepo.selectById(id)
        assertNotNull(before)

        val affected = accountRepo.updateById(
            id = id,
            query = AccountUpdateQuery(
                updatedAt = OffsetDateTime.now().plusSeconds(5),
            ),
        )

        assertEquals(1, affected)

        val account = accountRepo.selectById(id)
        assertNotNull(account)
        assertTrue(account.updatedAt.isAfter(before.updatedAt) || account.updatedAt.isEqual(before.updatedAt))
    }

}
