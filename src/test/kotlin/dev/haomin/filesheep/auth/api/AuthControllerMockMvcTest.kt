package dev.haomin.filesheep.auth.api

import java.util.UUID

import jakarta.servlet.http.Cookie

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.context.TestPropertySource

import dev.haomin.filesheep.TestcontainersConfiguration
import dev.haomin.filesheep.auth.REFRESH_TOKEN_COOKIE
import dev.haomin.filesheep.domain.account.AccountStatus
import dev.haomin.filesheep.domain.account.repo.AccountInsertQuery
import dev.haomin.filesheep.domain.account.repo.AccountRepo
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Transactional
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@TestPropertySource(properties = ["filesheep.auth.refresh.grace-time=0s"])
class AuthControllerMockMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var accountRepo: AccountRepo

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setUp(): Unit {
        dsl.execute("truncate table refresh_sessions, accounts cascade")
    }

    @Test
    fun loginRefreshAndLogoutFlow(): Unit {
        val email = FLOW_EMAIL
        val password = TEST_PASSWORD
        createActiveAccount(email = email, rawPassword = password)

        val loginResult = login(email = email, password = password, snippetName = "auth-login")
        val firstRefreshCookie = requireRefreshCookie(loginResult)
        assertTrue(firstRefreshCookie.isHttpOnly)
        assertTrue(firstRefreshCookie.maxAge > 0)
        assertNotNull(firstRefreshCookie.value)
        assertTrue(firstRefreshCookie.value.isNotBlank())

        val refreshResult = refresh(firstRefreshCookie.value, snippetName = "auth-refresh")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.tokens.access_token").isString)
            .andReturn()
        val secondRefreshCookie = requireRefreshCookie(refreshResult)
        assertTrue(secondRefreshCookie.maxAge > 0)
        assertNotNull(secondRefreshCookie.value)
        assertTrue(secondRefreshCookie.value.isNotBlank())

        logout(secondRefreshCookie.value, snippetName = "auth-logout")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .also {
                val clearCookie = requireRefreshCookie(it)
                assertEquals(0, clearCookie.maxAge)
            }
    }

    @Test
    fun refreshTokenReuseShouldBeRejected(): Unit {
        val email = REUSE_EMAIL
        val password = TEST_PASSWORD
        createActiveAccount(email = email, rawPassword = password)

        val loginResult = login(email = email, password = password)
        val oldRefreshToken = requireRefreshCookie(loginResult).value

        refresh(oldRefreshToken, snippetName = "auth-refresh-first-rotation")
            .andExpect(status().isOk)
            .andReturn()

        refresh(oldRefreshToken, snippetName = "auth-refresh-reuse-rejected")
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value(1101))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("reuse")))
    }

    private fun createActiveAccount(email: String, rawPassword: String): Unit {
        accountRepo.insert(
            AccountInsertQuery(
                id = UUID.randomUUID(),
                email = email,
                nick = "test-user",
                password = passwordEncoder.encode(rawPassword) ?: error("password encoder returned null"),
                status = AccountStatus.ACTIVE,
            ),
        )
    }

    private fun login(email: String, password: String, snippetName: String? = null): MvcResult {
        val result = mockMvc.perform(
            post("/api/auth/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password"
                    }
                    """.trimIndent()
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.payload.tokens.access_token").isString)

        if (snippetName != null) {
            result.andDo(document(snippetName))
        }
        return result.andReturn()
    }

    private fun refresh(refreshToken: String, snippetName: String? = null): ResultActions {
        val result = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(Cookie(REFRESH_TOKEN_COOKIE, refreshToken)),
        )
        if (snippetName != null) {
            result.andDo(document(snippetName))
        }
        return result
    }

    private fun logout(refreshToken: String, snippetName: String? = null): ResultActions {
        val result = mockMvc.perform(
            post("/api/auth/logout")
                .cookie(Cookie(REFRESH_TOKEN_COOKIE, refreshToken)),
        )
        if (snippetName != null) {
            result.andDo(document(snippetName))
        }
        return result
    }

    private fun requireRefreshCookie(result: MvcResult): Cookie =
        result.response.getCookie(REFRESH_TOKEN_COOKIE)
            ?: error("expected refresh cookie '$REFRESH_TOKEN_COOKIE' to be set")

    private companion object {
        const val FLOW_EMAIL: String = "flow-user@example.com"
        const val REUSE_EMAIL: String = "reuse-user@example.com"
        const val TEST_PASSWORD: String = "password-123"
    }
}
