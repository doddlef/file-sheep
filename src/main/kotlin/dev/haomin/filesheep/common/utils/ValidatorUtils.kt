package dev.haomin.filesheep.common.utils

import dev.haomin.filesheep.common.exception.InvalidParamException

/**
 * Shared input validation utilities.
 */
object ValidatorUtils {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val nicknameRegex = Regex("^[A-Za-z0-9_\\-\\s]+$")

    /**
     * Validates and normalizes email format.
     */
    fun validateEmail(email: String): Unit {
        val value = email.trim()
        if (value.isBlank()) {
            throw InvalidParamException("email cannot be blank")
        }
        if (value.length > 254) {
            throw InvalidParamException("email is too long")
        }
        if (!emailRegex.matches(value)) {
            throw InvalidParamException("email format is invalid")
        }
    }

    /**
     * Validates nickname.
     */
    fun validateNickname(nickname: String): Unit {
        val value = nickname.trim()
        if (value.isBlank()) {
            throw InvalidParamException("nickname cannot be blank")
        }
        if (value.length !in 2..40) {
            throw InvalidParamException("nickname length must be between 2 and 40")
        }
        if (!nicknameRegex.matches(value)) {
            throw InvalidParamException("nickname contains unsupported characters")
        }
    }

    /**
     * Validates password.
     */
    fun validatePassword(password: String): Unit {
        if (password.isBlank()) {
            throw InvalidParamException("password cannot be blank")
        }
        if (password.length !in 8..128) {
            throw InvalidParamException("password length must be between 8 and 128")
        }
    }
}
