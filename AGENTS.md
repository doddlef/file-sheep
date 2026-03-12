# Repository Guidelines

## Project Structure & Module Organization
- Source code lives in `src/main/kotlin/dev/haomin/filesheep`.
- Tests mirror production packages under `src/test/kotlin/dev/haomin/filesheep`.
- DB migrations are in `src/main/resources/db/migration` (Flyway).
- Local infrastructure is defined in `compose.yaml` (PostgreSQL + Redis).
- Design and architecture references are in `docs/` (especially `docs/DOMAIN.md` and `docs/contract.md`).
- jOOQ generated sources are in `build/generated-src/jooq/main/dev/haomin/filesheep/jooq` (`dev.haomin.filesheep.jooq` package).
- REST Docs snippets are generated in `build/generated-snippets`.
- Sensitive data, including DB credentials, is in `.env`.

Organize code by domain first (`auth`, `domain`, `infra`, `common`, `config`), then by layer.

## Build, Test, and Development Commands
- `./gradlew bootRun`: run the Spring Boot app locally.
- `./gradlew test`: run all JUnit 5 tests.
- `./gradlew test --tests dev.haomin.filesheep.infra.account.JooqAccountRepoTest`: run a single test class.
- `./gradlew compileKotlin`: compile Kotlin sources.
- `./gradlew flywayMigrate`: apply DB migrations (requires `POSTGRES_*` env vars).
- `./gradlew jooqCodegen`: regenerate jOOQ classes after schema changes.
  - Export environment variables first: `set -a && source .env && set +a`.
- `docker compose up -d`: start local PostgreSQL/Redis.
- `docker compose down`: stop local services.

## Coding Style & Naming Conventions
- Language: Kotlin (JDK toolchain configured in Gradle).
- Use tabs/formatting consistent with existing `.kts` and Kotlin files; keep functions small and explicit.
- Follow contract naming:
  - Service interface: `XxxService`; implementation: `XxxServiceImpl`
  - Repo interface: `XxxRepo`; jOOQ implementation: `JooqXxxRepo`
  - Service inputs: `XxxCmd`/`XxxQuery`; outputs: `XxxResult`/`XxxDetail`
- Keep controllers thin: map DTOs to service command objects; no business logic in controllers.
- Prefer composition over inheritance; keep business rules in service layer, not infra/controller.

## Testing Guidelines
- Frameworks: JUnit 5, Spring Boot Test, MockMvc, Testcontainers.
- Name tests `*Test` or `*MockMvcTest`; mirror package structure.
- Prioritize service logic, repository integration, and auth/permission flows.
- Use deterministic assertions; avoid network-dependent tests outside Testcontainers.
- For API tests, use `MockMvc` and assert both status and response contract.
- For data-layer tests, validate behavior against migrated schema.

## Commit & Pull Request Guidelines
- Use Conventional Commit style seen in history: `feat: ...`, `refactor: ...`, `init: ...`.
- Keep commits focused and atomic; include migration/codegen updates in the same change when relevant.
- PRs should include:
  - clear summary and scope
  - linked issue (if any)
  - test evidence (`./gradlew test` output summary)
  - API examples for controller changes (request/response snippets)
  - migration/codegen note when DB schema changes (`V*__*.sql` + regenerated jOOQ)

## Security & Configuration Tips
- Never trust client-provided identity fields; derive operator identity from a security context.
- Do not commit secrets; use `.env`/environment variables for DB and token settings.
- Keep token, refresh-session, and storage internals out of API response DTOs.
