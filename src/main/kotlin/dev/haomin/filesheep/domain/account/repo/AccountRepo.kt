package dev.haomin.filesheep.domain.account.repo

import dev.haomin.filesheep.domain.account.Account
import java.util.UUID

/**
 * Repository interface for account persistence operations.
 */
interface AccountRepo {

    /**
     * Retrieves an account by its unique identifier.
     *
     * @param id The unique identifier of the account to retrieve
     * @return The account associated with the given identifier, or null if no account exists
     */
    fun selectById(id: UUID): Account?

    /**
     * Retrieves an account by its associated email address.
     *
     * @param email The email address of the account to retrieve
     * @return The account associated with the given email, or null if no account exists
     */
    fun selectByEmail(email: String): Account?

    /**
     * Insert a new account into the database.
     *
     * @param query The account insert query containing the account data
     * @return The number of rows affected
     */
    fun insert(query: AccountInsertQuery): Int

    /**
     * Updates an existing account by its unique identifier.
     *
     * Only the fields provided in the query will be updated (partial update).
     *
     * @param id The unique identifier of the account to update
     * @param query The account update query containing the fields to update
     * @return The number of rows affected
     */
    fun updateById(id: UUID, query: AccountUpdateQuery): Int
}