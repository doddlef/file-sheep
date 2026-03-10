package dev.haomin.filesheep.infra.account

import dev.haomin.filesheep.domain.account.*
import dev.haomin.filesheep.domain.account.repo.*
import dev.haomin.filesheep.jooq.enums.E_AccountStatus
import dev.haomin.filesheep.jooq.tables.pojos.P_Accounts
import dev.haomin.filesheep.jooq.tables.references.ACCOUNTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * jOOQ implementation of [AccountRepo] for account persistence operations.
 */
@Repository
class JooqAccountRepo(
    private val dsl: DSLContext,
) : AccountRepo {

    override fun selectById(id: UUID): Account? =
        dsl.selectFrom(ACCOUNTS)
            .where(ACCOUNTS.id.eq(id))
            .fetchOneInto(P_Accounts::class.java)
            ?.toDomain()

    override fun selectByEmail(email: String): Account? =
        dsl.selectFrom(ACCOUNTS)
            .where(ACCOUNTS.email.eq(email))
            .fetchOneInto(P_Accounts::class.java)
            ?.toDomain()

    override fun insert(query: AccountInsertQuery): Int =
        dsl.newRecord(ACCOUNTS)
            .apply {
                this.id = query.id
                this.email = query.email
                this.nick = query.nick
                this.password = query.password
                this.status = query.status.toJooq()
                this.avatar = query.avatar
                this.createdAt = query.createdAt
                this.updatedAt = query.updatedAt
            }
            .let { dsl.insertInto(ACCOUNTS).set(it).execute() }

    override fun updateById(id: UUID, query: AccountUpdateQuery): Int =
        dsl.newRecord(ACCOUNTS)
            .apply {
                query.nick?.let { set(ACCOUNTS.nick, it) }
                query.password?.let { set(ACCOUNTS.password, it) }
                query.status?.let { set(ACCOUNTS.status, it.toJooq()) }
                query.avatar?.let { set(ACCOUNTS.avatar, it) }
                set(ACCOUNTS.updatedAt, query.updatedAt)
            }
            .let {
                if (it.modified())
                    dsl.update(ACCOUNTS).set(it).where(ACCOUNTS.id.eq(id)).execute()
                else
                    0
            }
    }

/**
 * Converts an instance of the `AccountStatus` enum to its corresponding `E_AccountStatus` enum.
 *
 * @return The `E_AccountStatus` value that corresponds to the current `AccountStatus` enum value.
 * @throws IllegalArgumentException if the conversion fails due to an invalid name.
 */
internal fun AccountStatus.toJooq(): E_AccountStatus = E_AccountStatus.valueOf(name)

/**
 * Converts an instance of the `E_AccountStatus` enum to the corresponding `AccountStatus` enum.
 *
 * @return The `AccountStatus` enum value that matches the name of the `E_AccountStatus` instance.
 * @throws IllegalArgumentException if no matching `AccountStatus` value is found.
 */
internal fun E_AccountStatus.toDomain(): AccountStatus = AccountStatus.fromString(name)

/**
 * Maps a `P_Accounts` instance to an `Account` domain model instance.
 *
 * Converts the properties of the `P_Accounts` data class into their corresponding
 * counterparts in the `Account` data class. Ensures that all non-nullable fields
 * in the `Account` model are populated by verifying their presence in the source.
 *
 * @return A fully populated `Account` instance created from the `P_Accounts` data.
 * @throws IllegalArgumentException if any required field in the source is null.
 */
internal fun P_Accounts.toDomain(): Account =
    Account(
        id = requireNotNull(id) { "Account id cannot be null" },
        email = requireNotNull(email) { "Account email cannot be null" },
        nick = requireNotNull(nick) { "Account nick cannot be null" },
        password = requireNotNull(password) { "Account password cannot be null" },
        status = requireNotNull(status) { "Account status cannot be null" }.toDomain(),
        avatar = avatar,
        createdAt = requireNotNull(createdAt) { "Account createdAt cannot be null" },
        updatedAt = requireNotNull(updatedAt) { "Account updatedAt cannot be null" },
    )

