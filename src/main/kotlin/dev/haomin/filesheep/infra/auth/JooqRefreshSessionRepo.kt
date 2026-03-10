package dev.haomin.filesheep.infra.auth

import java.util.UUID

import org.jooq.DSLContext
import org.springframework.stereotype.Repository

import dev.haomin.filesheep.domain.auth.RefreshSession
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionInsertQuery
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionRepo
import dev.haomin.filesheep.domain.auth.repo.RefreshSessionUpdateQuery
import dev.haomin.filesheep.jooq.tables.pojos.P_RefreshSessions
import dev.haomin.filesheep.jooq.tables.references.REFRESH_SESSIONS

/**
 * jOOQ implementation of [RefreshSessionRepo] for refresh session persistence operations.
 */
@Repository
class JooqRefreshSessionRepo(
    private val dsl: DSLContext,
) : RefreshSessionRepo {

    override fun selectByIdForUpdate(id: UUID): RefreshSession? =
        dsl.selectFrom(REFRESH_SESSIONS)
            .where(REFRESH_SESSIONS.id.eq(id))
            .forUpdate()
            .fetchOneInto(P_RefreshSessions::class.java)
            ?.toDomain()

    override fun insert(query: RefreshSessionInsertQuery): Int =
        dsl.newRecord(REFRESH_SESSIONS)
            .apply {
                this.id = query.id
                this.accountId = query.accountId
                this.token = query.token
                this.createdAt = query.createdAt
                this.updatedAt = query.updatedAt
            }
            .let { dsl.insertInto(REFRESH_SESSIONS).set(it).execute() }

    override fun updateById(id: UUID, query: RefreshSessionUpdateQuery): Int =
        dsl.newRecord(REFRESH_SESSIONS)
            .apply {
                query.token?.let { set(REFRESH_SESSIONS.token, it) }
                query.prevToken?.let { set(REFRESH_SESSIONS.prevToken, it) }
                query.lastUsedAt?.let { set(REFRESH_SESSIONS.lastUsedAt, it) }
                query.revokedAt?.let { set(REFRESH_SESSIONS.revokedAt, it) }
                query.revokeReason?.let { set(REFRESH_SESSIONS.revokeReason, it) }
                set(REFRESH_SESSIONS.updatedAt, query.updatedAt)
            }
            .let {
                if (it.modified())
                    dsl.update(REFRESH_SESSIONS).set(it).where(REFRESH_SESSIONS.id.eq(id)).execute()
                else
                    0
            }
}

/**
 * Maps a [P_RefreshSessions] instance to a [RefreshSession] domain model.
 */
internal fun P_RefreshSessions.toDomain(): RefreshSession =
    RefreshSession(
        id = requireNotNull(id) { "RefreshSession id cannot be null" },
        accountId = requireNotNull(accountId) { "RefreshSession accountId cannot be null" },
        token = requireNotNull(token) { "RefreshSession token cannot be null" },
        prevToken = prevToken,
        lastUsedAt = lastUsedAt,
        revokedAt = revokedAt,
        revokeReason = revokeReason,
        createdAt = requireNotNull(createdAt) { "RefreshSession createdAt cannot be null" },
        updatedAt = requireNotNull(updatedAt) { "RefreshSession updatedAt cannot be null" },
    )
