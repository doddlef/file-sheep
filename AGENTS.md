# AGENTS.md - File Sheep Development Guide

Guidelines for agents working on the File Sheep codebase.

---

## 1. Project Overview

- **Type**: Kotlin Spring Boot application (REST API)
- **Build System**: Gradle (Kotlin DSL)
- **JDK**: 21 | **Framework**: Spring Boot 4.0.3
- **Package**: `dev.haomin.filesheep`

---

## 2. Build, Lint, and Test Commands

### Gradle
```bash
./gradlew          # Unix/macOS
./gradlew.bat      # Windows
./gradlew build              # Build (compile + test)
./gradlew build -x test      # Build without tests
./gradlew bootJar            # Create JAR
./gradlew bootRun            # Run application
./gradlew clean build        # Clean and rebuild
./gradlew jooqCodegen        # Generate jOOQ from schema
```

### Testing
```bash
./gradlew test               # Run all tests
./gradlew test --info        # Verbose output

# Run single test class
./gradlew test --tests "dev.haomin.filesheep.FilesheepApplicationTests"

# Run single test method
./gradlew test --tests "dev.haomin.filesheep.FilesheepApplicationTests.contextLoads"
```

---

## 3. Code Style

### Package Structure
```
dev.haomin.filesheep.common.*       - shared utilities
dev.haomin.filesheep.feature.*     - feature modules
dev.haomin.filesheep.domain.*       - domain models
dev.haomin.filesheep.infrastructure.* - infrastructure code
```

### Naming

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `FilesheepApplication` |
| Functions | camelCase | `createResponse()` |
| Properties | camelCase | `userId`, `isActive` |
| Constants | UPPER_SNAKE_CASE | `MAX_FILE_SIZE` |
| Enums | PascalCase | `ResponseCode` |
| Test classes | `*Test` or `*IT` | `FilesheepApplicationTests` |

### Formatting
- **Indentation**: 4 spaces | **Line length**: ~120 chars
- **Blank lines**: single between declarations, double between functions

### Imports (in order)
1. Kotlin stdlib (`kotlin.*`)
2. Java stdlib (`java.*`, `javax.*`)
3. Spring (`org.springframework.*`)
4. Third-party
5. Project (`dev.haomin.filesheep.*`)

Use explicit imports (no wildcard `*` unless same package).

### Types
- Use `data class` with `val` for immutable data
- Nullable: `String?` | Non-null: `String`
- Use interfaces: `List`, `Map`, `Set` (not concrete types)
- Explicit return types on public functions

### Data Classes
```kotlin
data class ApiResponse(
    val code: Int,
    val message: String? = null,
    val payload: Map<String, Any?>? = null,
)
```

### Enums
```kotlin
enum class ResponseCode(
    val code: Int,
    val description: String,
    val status: HttpStatus
) {
    SUCCESS(0, "success", HttpStatus.OK),
    FAILURE(1000, "something went wrong", HttpStatus.INTERNAL_SERVER_ERROR),
}
```

### KDoc
Use for all public classes, functions, and complex properties:
```kotlin
/** Standard API response structure */
data class ApiResponse(...)
```

### Error Handling
- Custom exceptions for domain errors
- Use `ResponseCode` enum for standardized errors
- Return appropriate HTTP status codes

### Builder Pattern
```kotlin
fun success(message: String = "OK") =
    builder(ResponseCode.SUCCESS).message(message)
```

### Dependency Injection
- Constructor injection (primary)
- Use `@Service`, `@Repository`, `@Component`
- Use `@ConfigurationProperties` for config binding

### Testing
- Test files in `src/test/kotlin` mirroring main structure
- Use `@SpringBootTest` for integration tests
- Use `@TestcontainersConfiguration` for Docker-based tests (PostgreSQL, Redis)

```kotlin
@Import(TestcontainersConfiguration::class)
@SpringBootTest
class FilesheepApplicationTests {
    @Test
    fun contextLoads() {}
}
```

---

## 4. Database

- **PostgreSQL** via jOOQ | **Migrations**: Flyway
- Run `./gradlew jooqCodegen` after schema changes
- Generated code: `build/generated-src/jooq/main` (do not edit)

---

## 5. Dependencies

| Category | Library |
|----------|---------|
| Web | Spring Boot Web MVC |
| Database | jOOQ, PostgreSQL, Flyway |
| Cache | Spring Cache, Caffeine |
| Security | Spring Security, JJWT |
| Redis | Lettuce |
| Testing | JUnit 5, Testcontainers |

---

## 6. Important Notes

- Never commit secrets—use `.env` (already in `.gitignore`)
- jOOQ generated code must not be manually edited
- Follow domain model in `README.md`
- All file operations must enforce authentication and ownership checks
