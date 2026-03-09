# AGENTS.md - FileSheep Development Guide

## Project Overview

FileSheep is a Kotlin/Spring Boot cloud file drive application using:
- **Language**: Kotlin 2.2.21
- **JDK**: 21
- **Framework**: Spring Boot 4.0.3
- **Database**: PostgreSQL with jOOQ
- **Cache**: Redis with Caffeine
- **Build Tool**: Gradle (Kotlin DSL)
- **Testing**: JUnit 5 with Testcontainers

---

## Build & Run Commands

### Development
```bash
# Run application
./gradlew bootRun

# Build without tests
./gradlew build -x test
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "FilesheepApplicationTests"

# Run a specific test method
./gradlew test --tests "FilesheepApplicationTests.contextLoads"

# Run tests with verbose output
./gradlew test --info
```

### Database
```bash
# Start infrastructure (PostgreSQL, Redis)
docker compose up -d

# Generate jOOQ code (requires running PostgreSQL)
./gradlew jooqCodegen
```

---

## Code Style Guidelines

### General Principles
- Use **4 spaces** for indentation (Kotlin default)
- No trailing whitespace
- Maximum line length: 120 characters (soft limit)
- Use meaningful, descriptive names

### Naming Conventions
- **Classes**: PascalCase (`UserService`, `FileController`)
- **Functions/variables**: camelCase (`getUserById`, `fileName`)
- **Constants**: UPPER_SNAKE_CASE
- **Packages**: lowercase, single words (`dev.haomin.filesheep`)
- **Test Classes**: suffix with `Tests` or `Test` (`UserServiceTests`)

### Imports
- Kotlin standard library imports first
- Spring framework imports
- Third-party library imports
- Project imports
- Use explicit imports (no wildcard `*`)

### Types & Null Safety
- Use Kotlin's null safety features (`?`, `?.`, `?:`, `let`)
- Prefer `val` over `var`
- Use data classes for DTOs and entities
- Use `sealed class` for sealed hierarchies

### Functions
- Single-expression functions: `fun foo() = bar`
- Use default parameters instead of overloads
- Keep functions small and focused

### Classes
- Use primary constructors
- Prefer composition over inheritance
- Use `object` for singletons

### Error Handling
- Use exceptions for exceptional cases
- Use `Result<T>` or sealed classes for expected failures
- Don't catch generic `Exception` unless necessary
- Use meaningful error messages

### Testing
- Use `@SpringBootTest` for integration tests
- Use `@Testcontainers` for database/Redis tests
- Follow AAA pattern: Arrange, Act, Assert
- One assertion per test method when possible

### Database & jOOQ
- Generated jOOQ classes are in `build/generated-src/jooq/main`
- Custom queries go in repository classes
- Use transactions with `@Transactional`

### Configuration
- Use `application.yml` for configuration
- Use `@ConfigurationProperties` for type-safe config
- Never commit secrets to version control

---

## Project Structure

```
src/
├── main/
│   ├── kotlin/dev.haomin.filesheep/
│   │   ├── [domain]/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── model/
│   │   └── FilesheepApplication.kt
│   └── resources/
│       └── application.yml
└── test/
    └── kotlin/dev.haomin.filesheep/
        ├── TestcontainersConfiguration.kt
        └── [domain]/*Tests.kt
```

---

## Common Tasks

### Adding a new dependency
1. Add version to `gradle/libs.versions.toml` under `[versions]`
2. Add library under `[libraries]`
3. Add to `build.gradle.kts` dependencies block

### Creating a new controller
```kotlin
@RestController
@RequestMapping("/api/v1/resources")
class ResourceController(
    private val service: ResourceService
) {
    @GetMapping
    fun list(): List<Resource> = service.findAll()
}
```

### Creating a new test
```kotlin
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class ResourceServiceTests {
    @Test
    fun `should find all resources`() {
        // Arrange
        val service = ResourceService(...)
        
        // Act
        val result = service.findAll()
        
        // Assert
        assertThat(result).isNotEmpty()
    }
}
```
