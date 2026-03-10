package dev.haomin.filesheep.auth.service.impl

import java.security.SecureRandom

import org.springframework.stereotype.Component

import dev.haomin.filesheep.auth.service.RegisterCodeGenerator
import dev.haomin.filesheep.common.exception.InvalidParamException

/**
 * Secure random generator for numeric register verification codes.
 */
@Component
class SecureNumericRegisterCodeGenerator : RegisterCodeGenerator {

    private val random: SecureRandom = SecureRandom()

    override fun generate(length: Int): String {
        if (length <= 0) {
            throw InvalidParamException("verification code length must be positive")
        }

        return buildString(length) {
            repeat(length) {
                append(random.nextInt(10))
            }
        }
    }
}
