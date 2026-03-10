package dev.haomin.filesheep.domain.auth.repo

import dev.haomin.filesheep.domain.auth.RefreshSession
import java.util.UUID

/**
 * Repository interface for refresh session persistence operations.
 */
interface RefreshSessionRepo {

    /**
     * Retrieves a refresh session by id and locks the row for update.
     *
     * @param id The refresh session id
     * @return The refresh session if found, otherwise null
     */
    fun selectByIdForUpdate(id: UUID): RefreshSession?

    /**
     * Inserts a new refresh session.
     *
     * @param query The insert query
     * @return The number of rows affected
     */
    fun insert(query: RefreshSessionInsertQuery): Int

    /**
     * Partially updates a refresh session by id.
     *
     * @param id The refresh session id
     * @param query The update query containing fields to modify
     * @return The number of rows affected
     */
    fun updateById(id: UUID, query: RefreshSessionUpdateQuery): Int
}
