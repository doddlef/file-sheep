package dev.haomin.filesheep.domain.account

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Account profile data transfer object.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AccountProfile(
    val id: UUID,
    val email: String,
    val nick: String,
    val avatar: String?,
    val status: AccountStatus,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun from(account: Account): AccountProfile =
            AccountProfile(
                id = account.id,
                email = account.email,
                nick = account.nick,
                avatar = account.avatar,
                status = account.status,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt,
            )
    }
}