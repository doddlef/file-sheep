# MockMvc Testing Guidance

## Goal

Write MockMvc tests in a style that is:

- readable
- modern
- efficient
- stable
- easy to maintain

This guidance is for Spring MVC controller testing in a modern Spring Boot project.

---

## Recommended Testing Levels

Use the smallest test scope that still proves the behavior you care about.

### 1. Controller slice tests

Prefer:

```kotlin
@WebMvcTest(YourController::class)
```

Use this when you want to test:

- request mapping
- request validation
- JSON input/output
- status codes
- headers
- cookies
- Spring Security behavior at the web layer

Mock service dependencies with `@MockBean`.

This is usually the best default for controller tests because it is faster and more focused than loading the full application.

---

### 2. Full integration-style web tests

Use:

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
```

Use this when you want to test a larger flow involving:

- controller
- service
- repository
- security filters
- database
- redis
- real configuration

This is appropriate for end-to-end auth flows like:

- login
- refresh
- logout
- token rotation
- cookie behavior

Do not use full-context tests for every simple controller case.

---

## Modern Style Preferences

## 1. Prefer readable request bodies

For very small JSON payloads, it is acceptable and often clearer to write JSON directly.

Prefer:

```kotlin
.content(
    """
    {
      "email": "flow-user@example.com",
      "password": "password-123"
    }
    """.trimIndent()
)
```

instead of:

```kotlin
.content(
    objectMapper.writeValueAsString(
        mapOf("email" to email, "password" to password)
    )
)
```

### Why

Direct JSON is often better when:

- the payload is very small
- the structure is obvious
- the test is about HTTP behavior, not object serialization
- you want the request body to be instantly readable inside the test

### When not to use raw JSON

Prefer typed serialization when:

- request payload is large
- request object is reused across tests
- field names are easy to mistype
- serialization rules matter
- you want refactor safety

In that case, prefer request DTOs over `mapOf(...)`.

Better:

```kotlin
data class LoginRequest(
    val email: String,
    val password: String,
)
```

and then:

```kotlin
.content(objectMapper.writeValueAsString(LoginRequest(email, password)))
```

### Style rule

For tiny one-off payloads:
- raw JSON string is fine

For reusable or non-trivial payloads:
- prefer request DTO serialization

Avoid `mapOf(...)` as the default style for request JSON because it is less type-safe and becomes messy as payloads grow.

---

## 2. Prefer direct cookie access from response

When a response sets cookies, do not manually parse `Set-Cookie` unless you truly need the raw header string.

Prefer reading the cookie directly from the response.

Example:

```kotlin
val result = mockMvc.perform(
    post("/api/auth/")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {
              "email": "flow-user@example.com",
              "password": "password-123"
            }
            """.trimIndent()
        )
)
    .andExpect(status().isOk)
    .andReturn()

val refreshCookie = result.response.getCookie(REFRESH_TOKEN_COOKIE)
checkNotNull(refreshCookie)
val refreshToken = refreshCookie.value
```

### Why

This is better than manually extracting from `Set-Cookie` because:

- less fragile
- no string parsing
- easier to read
- easier to extend with assertions like:
    - `httpOnly`
    - `secure`
    - `path`
    - `maxAge`

Example:

```kotlin
assertThat(refreshCookie.isHttpOnly).isTrue()
assertThat(refreshCookie.secure).isTrue()
assertThat(refreshCookie.path).isEqualTo("/api/auth")
```

Use raw `Set-Cookie` header assertions only when you specifically want to verify header formatting or attributes that are easiest to inspect as a header string.

---

## 3. Prefer helper functions for repetitive flows

For auth tests, repeated flow steps should be extracted into small helpers.

Example:

```kotlin
private fun login(email: String, password: String) =
    mockMvc.perform(
        post("/api/auth/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "email": "$email",
                  "password": "$password"
                }
                """.trimIndent()
            )
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.code").value(0))
        .andReturn()

private fun refresh(refreshToken: String) =
    mockMvc.perform(
        post("/api/auth/refresh")
            .cookie(Cookie(REFRESH_TOKEN_COOKIE, refreshToken))
    )
```

Then the test becomes much cleaner:

```kotlin
val loginResult = login(email, password)
val firstRefreshToken = loginResult.response.getCookie(REFRESH_TOKEN_COOKIE)!!.value

val refreshResult = refresh(firstRefreshToken)
    .andExpect(status().isOk)
    .andReturn()

val secondRefreshToken = refreshResult.response.getCookie(REFRESH_TOKEN_COOKIE)!!.value
```

---

## 4. Prefer focused assertions

Each test should verify only the behavior that matters for that scenario.

Good assertions:

- HTTP status
- response code/message
- expected JSON fields
- expected cookie existence
- expected cookie replacement
- expected failure code on invalid flow

Avoid asserting too many unrelated details in one test.

---

## 5. Prefer stable time handling over `Thread.sleep(...)`

Avoid:

```kotlin
Thread.sleep(6000)
```

This makes tests slower and more brittle.

Prefer one of these:

### Option A: injectable clock
Inject a `Clock` into the service and use a fixed or mutable test clock.

### Option B: configurable grace/reuse window
Set a very short value in test properties.

### Option C: explicit test hook
If the workflow depends on time, make time testable rather than waiting in real time.

This is especially important for:
- token expiration
- refresh reuse grace period
- cooldown testing

---

## 6. Prefer constants and named fixtures

Avoid magic literals scattered through tests.

Prefer:

```kotlin
private val TEST_EMAIL = "flow-user@example.com"
private val TEST_PASSWORD = "password-123"
```

This improves readability and reduces accidental inconsistencies.

---

## 7. Prefer clear test naming

Test names should describe behavior, not implementation detail.

Prefer names like:

```kotlin
fun `login should return access token and refresh cookie`()
```

These are clearer than vague names like:

```kotlin
fun testLogin()
fun authFlow()
```

---

## 8. Prefer AssertJ when possible

Modern Spring docs support `MockMvcTester`, which builds on MockMvc and gives a cleaner AssertJ-style API. This is part of the current Spring testing guidance and is a good option for newer codebases.  [oai_citation:1‡Home](https://docs.spring.io/spring-framework/reference/testing/mockmvc/assertj.html?utm_source=chatgpt.com)

If the project is already heavily invested in classic MockMvc, it is fine to continue using it. But for new tests, consider using `MockMvcTester` for more fluent assertions.

Example style:

```kotlin
assertThat(mockMvcTester.post().uri("/api/auth/refresh"))
    .hasStatusOk()
```

This is optional, not mandatory.

---

## 9. Prefer `@WebMvcTest` for controller-only behavior

Spring Boot documents test slices for narrower testing, and `@SpringBootTest` plus `@AutoConfigureMockMvc` is best reserved for broader flows that truly need the full context.  [oai_citation:2‡Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com)

Use `@WebMvcTest` when testing:

- request validation
- serialization/deserialization
- controller advice mapping
- security at the controller boundary

Use full-context MockMvc tests only for true integration flows.

---

## 10. Prefer verifying cookie behavior explicitly

For auth endpoints, cookies are part of the contract. Assert them directly.

Example:

```kotlin
val cookie = result.response.getCookie(REFRESH_TOKEN_COOKIE)
checkNotNull(cookie)

assertThat(cookie.value).isNotBlank()
assertThat(cookie.isHttpOnly).isTrue()
assertThat(cookie.maxAge).isNotEqualTo(0)
```

For logout:

```kotlin
val cookie = result.response.getCookie(REFRESH_TOKEN_COOKIE)
checkNotNull(cookie)

assertThat(cookie.maxAge).isZero()
```

This is cleaner than relying only on `Set-Cookie` substring matching.

---

## Suggested Style for AuthControllerMockMvcTest

For this project, prefer the following improvements.

### Use direct JSON for tiny request bodies

Replace:

```kotlin
.content(
    objectMapper.writeValueAsString(
        mapOf("email" to email, "password" to password),
    )
)
```

with:

```kotlin
.content(
    """
    {
      "email": "$email",
      "password": "$password"
    }
    """.trimIndent()
)
```

Or use a request DTO if you want type safety.

---

### Use response cookie access instead of manual extraction

Replace manual parsing logic like:

```kotlin
val loginSetCookie = loginResult.response.getHeader(HttpHeaders.SET_COOKIE)
val firstRefreshToken = extractCookieValue(loginSetCookie, REFRESH_TOKEN_COOKIE)
```

with:

```kotlin
val firstRefreshCookie = loginResult.response.getCookie(REFRESH_TOKEN_COOKIE)
checkNotNull(firstRefreshCookie)
val firstRefreshToken = firstRefreshCookie.value
```

Then delete custom `extractCookieValue(...)` if it is no longer needed.

---

### Extract reusable auth helpers

Example helper methods:

```kotlin
private fun login(email: String, password: String) = ...
private fun refresh(refreshToken: String) = ...
private fun logout(refreshToken: String) = ...
private fun createActiveAccount(email: String, rawPassword: String) = ...
```

This keeps each test focused on scenario logic.

---

### Avoid real waiting when testing token reuse windows

Replace:

```kotlin
Thread.sleep(6000)
```

If possible:
- reduced test property window

---

## Example Preferred Test Style

```kotlin
@Test
fun `refresh with reused old roken should be rejected`() {
    val email = "reuse-user@example.com"
    val password = "password-123"
    createActiveAccount(email, password)

    val loginResult = login(email, password)
    val oldRefreshToken = loginResult.response.getCookie(REFRESH_TOKEN_COOKIE)!!.value

    refresh(oldRefreshToken)
        .andExpect(status().isOk)

    advanceReuseWindowIfNeeded()

    refresh(oldRefreshToken)
        .andExpect(status().isUnauthorized)
        .andExpect(jsonPath("$.code").value(1101))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("reuse")))
}
```

---

## Final Guidance

Default style:

- use `@WebMvcTest` for controller slice tests
- use `@SpringBootTest + @AutoConfigureMockMvc` only for broader auth/integration flows
- use direct JSON strings for tiny payloads
- use DTO serialization for larger payloads
- avoid `mapOf(...)` as the normal request-body style
- read cookies with `response.getCookie(...)`
- extract flow helpers
- avoid `Thread.sleep(...)`
- consider `MockMvcTester` for newer AssertJ-style tests

This should be the preferred MockMvc testing style for modern Spring Boot code.