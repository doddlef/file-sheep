package dev.haomin.filesheep.auth.service

/**
 * Generator for numeric verification codes.
 */
interface RegisterCodeGenerator {

    /**
     * Generates a numeric code with exact length.
     */
    fun generate(length: Int): String
}
