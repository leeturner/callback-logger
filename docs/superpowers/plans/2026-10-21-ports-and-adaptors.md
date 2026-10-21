# Ports and Adaptors Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure callback-logger into encore-web-api's ports-and-adaptors shape with no behaviour change.

**Architecture:** A Micronaut-free `domain/callbacks` package (models, error types, one `fun interface` port per
operation) and `adapters/inbound/web` + `adapters/outbound/db`. Controllers call ports directly; `CallbackService`
goes away. ArchUnit enforces the boundaries.

**Tech Stack:** Kotlin 2.4, Micronaut 5 (Data JDBC, H2, Thymeleaf views), Arrow core, Strikt (+ strikt-arrow),
ArchUnit, JUnit 6, RestAssured.

**Spec:** `docs/superpowers/specs/2026-10-21-ports-and-adaptors-design.md`

**Reference app:** `~/dev/encore/encore-web-api` — copy its idioms (port KDoc, adapter `either { catch(...) }`,
`companion object` slf4j logger, ArchitectureTest).

## Global Constraints

- No behaviour change: endpoints, page, config keys, status codes, response bodies and the `println` dump in
  `CallbackApiController` stay as they are. Existing integration tests keep their assertions; they move/re-point only.
- Nothing under `com.leeturner.callback_logger.domain` imports `io.micronaut..`, `jakarta..` or serde.
- Deps inline in `build.gradle.kts`: `io.arrow-kt:arrow-core:2.2.3`, `io.strikt:strikt-arrow:0.35.1`,
  `com.tngtech.archunit:archunit:1.5.1`.
- H2 with `create_drop`, table name stays `callback`.
- A `Left` from any port → controller logs `logger.error("Failed to <op>: {}", error)` and returns HTTP 500.
- Commit with `git commit --no-gpg-sign`, ending the message with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Full suite command: `./gradlew test` — green before and after every task (baseline: green, 10 test classes).

## Review Focus

1. DB failure while saving a POST/PUT callback (including custom routes) → 500, not 200 with the configured payload. (Task 2)
2. DB failure loading the inbox page → 500. (Task 2)
3. DB failure on `/clear` → 500, no redirect. (Task 2)
4. Payloads longer than 255 chars still save: `DbCallback.payload` must keep `@MappedProperty(definition = "CLOB")`. (Task 1)
5. Custom-route callbacks are still persisted, not just answered with 200. (Task 2)

---

### Task 1: Domain, ports and database adapters

**Files:**
- Modify: `build.gradle.kts` (add the three deps from Global Constraints)
- Create: `src/main/kotlin/com/leeturner/callback_logger/domain/callbacks/Callback.kt`
- Create: `src/main/kotlin/com/leeturner/callback_logger/domain/callbacks/NewCallback.kt`
- Move: `callbacks/CallbackStatus.kt` → `domain/callbacks/CallbackStatus.kt` (content unchanged bar package)
- Create: `domain/callbacks/SaveCallbackError.kt`, `ListCallbacksError.kt`, `ClearCallbacksError.kt`
- Create: `domain/callbacks/ports/SaveCallbackPort.kt`, `ListCallbacksPort.kt`, `ClearCallbacksPort.kt`
- Move+rename: `callbacks/Callback.kt` → `adapters/outbound/db/DbCallback.kt`
- Move+rename: `callbacks/CallbackRepository.kt` → `adapters/outbound/db/DbCallbackRepository.kt`
- Create: `adapters/outbound/db/DatabaseCallbackSaverAdapter.kt`, `DatabaseCallbackListerAdapter.kt`,
  `DatabaseCallbackClearerAdapter.kt`
- Modify: `callbacks/CallbackService.kt` (temporary bridge onto the ports; deleted in Task 2)
- Delete: `src/test/.../callbacks/CallbackRepositoryTest.kt`
- Create: `src/test/kotlin/com/leeturner/callback_logger/adapters/outbound/db/DatabaseCallbackAdaptersTest.kt`
- Modify: `src/test/.../TestFixtures.kt` and every test injecting `CallbackRepository` → `DbCallbackRepository`

(All main paths above are under `src/main/kotlin/com/leeturner/callback_logger/`.)

**Interfaces:**
- Produces (package `com.leeturner.callback_logger.domain.callbacks`):
  - `data class Callback(val id: Long, val timestamp: LocalDateTime, val status: CallbackStatus, val uri: String, val httpMethod: String, val httpVersion: String, val payload: String)`
  - `data class NewCallback(val uri: String, val httpMethod: String, val httpVersion: String, val payload: String)`
  - `sealed interface SaveCallbackError { data class PersistenceError(val message: String) : SaveCallbackError }` — same shape for `ListCallbacksError`, `ClearCallbacksError`
- Produces (package `...domain.callbacks.ports`), each a `fun interface` with brief KDoc like encore's:
  - `SaveCallbackPort.save(newCallback: NewCallback): Either<SaveCallbackError, Callback>`
  - `ListCallbacksPort.listNewestFirst(): Either<ListCallbacksError, List<Callback>>`
  - `ClearCallbacksPort.clear(): Either<ClearCallbacksError, Unit>`
- Produces (package `...adapters.outbound.db`):
  - `@MappedEntity("callback") data class DbCallback(@field:Id @GeneratedValue val id: Long? = null, val timestamp: LocalDateTime = LocalDateTime.now(), val status: CallbackStatus = CallbackStatus.NEW, val uri: String, val httpMethod: String, val httpVersion: String, @MappedProperty(definition = "CLOB") val payload: String)`
  - `internal fun DbCallback.toDomain(): Callback` (in `DbCallback.kt`; `requireNotNull(id) { "DbCallback.id must be set; called toDomain() on an unsaved entity?" }`)
  - `@JdbcRepository(dialect = Dialect.H2) interface DbCallbackRepository : CrudRepository<DbCallback, Long> { fun listOrderByTimestampDesc(): List<DbCallback> }`
  - `@Singleton` `DatabaseCallbackSaverAdapter(repository: DbCallbackRepository) : SaveCallbackPort`, likewise `…ListerAdapter : ListCallbacksPort`, `…ClearerAdapter : ClearCallbacksPort`

- [ ] **Step 1: Add the three dependencies to `build.gradle.kts`**

`arrow-core` as `implementation`, the other two as `testImplementation`, next to the existing strikt line.

- [ ] **Step 2: Write the failing adapter test**

`DatabaseCallbackAdaptersTest`, annotated `@MicronautTest(environments = [Environment.TEST], startApplication = false)`,
injecting the three adapters and `DbCallbackRepository`; `@BeforeEach` calls `repository.deleteAll()`. Use
`strikt.arrow.isRight`. Fixture: `NewCallback(uri = "/api/callback", httpMethod = "POST", httpVersion = "HTTP_1.1", payload = TEST_JSON_PAYLOAD)`.

- `save stores a callback with NEW status` — `isRight().get { value.status } isEqualTo CallbackStatus.NEW`, and `value.id` > 0
- `save stores a callback with a timestamp of now` — `value.timestamp` > `LocalDateTime.now().minusMinutes(1)`
- `save stores the callback values` — `uri`, `httpMethod`, `httpVersion`, `payload` equal the fixture's
- `save stores payloads longer than a varchar` — payload `"x".repeat(10_000)` round-trips through `listNewestFirst()` unchanged
- `listNewestFirst returns the most recent first` — seed via `repository.save(DbCallback(...))` twice, second with
  `timestamp = LocalDateTime.now().plusMinutes(10)`; `listNewestFirst()` → `isRight()`, ids `containsExactly(second.id, first.id)`
- `clear removes every callback` — save two via the port, `clear()` is Right, `listNewestFirst()` is Right and empty

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew test --tests '*DatabaseCallbackAdaptersTest'`
Expected: compilation FAIL (unresolved `NewCallback`, `DatabaseCallbackSaverAdapter`, …)

- [ ] **Step 4: Create the domain types, ports, `DbCallback`, `DbCallbackRepository` and the three adapters per Interfaces**

Delete `callbacks/Callback.kt` and `callbacks/CallbackRepository.kt` (two entities on table `callback` would clash).
Each adapter body is encore's `either { catch({ … }) { e: Exception -> raise(XError.PersistenceError(e.message ?: "Unknown persistence error")) } }`.
Saver builds `DbCallback(uri, httpMethod, httpVersion, payload)` (defaults supply timestamp/status) and returns
`repository.save(...).toDomain()`.

- [ ] **Step 5: Bridge `CallbackService` onto the ports so the app still compiles and runs**

Constructor takes the three ports instead of the repository; public method signatures unchanged. Unwrap with
`getOrElse { throw IllegalStateException(it.toString()) }` (throwaway: Task 2 deletes the service). `toDto()` uses
`id` directly.

- [ ] **Step 6: Re-point the remaining tests**

Delete `CallbackRepositoryTest.kt` (its cases now live in `DatabaseCallbackAdaptersTest`). In `TestFixtures`,
`TEST_JSON_CALLBACK` / `TEST_XML_CALLBACK` become `DbCallback`s with the same values. Every test constructor param
`callbackRepository: CallbackRepository` becomes `DbCallbackRepository`; the API controller tests' `CallbackStatus`
import moves to the domain package. No assertions change.

- [ ] **Step 7: Run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, `DatabaseCallbackAdaptersTest` 6 passing.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit --no-gpg-sign -m "Add callback domain, ports and database adapters"
```

---

### Task 2: Inbound web adapter calls the ports directly

**Files:**
- Move: `callbacks/CallbackApiController.kt`, `CallbackWebController.kt`, `CustomCallbackRoutes.kt`,
  `CustomCallbackConfiguration.kt` → `adapters/inbound/web/`
- Move+rename: `callbacks/CallbackViewDto.kt` → `adapters/inbound/web/CallbackView.kt`
- Delete: `callbacks/CallbackService.kt` (the `callbacks` main package is now empty)
- Move: every test in `src/test/.../callbacks/` → `src/test/.../adapters/inbound/web/` (package line only)
- Create: `src/test/kotlin/com/leeturner/callback_logger/adapters/inbound/web/PersistenceFailureTest.kt`
- Modify: `src/test/.../adapters/inbound/web/CustomCallbackTests.kt`

**Interfaces:**
- Consumes: the three ports and error types from Task 1.
- Produces:
  - `@Serdeable data class CallbackView(id: Long, status: String, uri: String, timestamp: LocalDateTime, httpMethod: String, httpVersion: String, payload: String)` with the existing `formattedTimestamp` getter
  - `fun Callback.toView(): CallbackView` (same file; `status = status.name`)
  - `CallbackApiController(saveCallbackPort: SaveCallbackPort, <the two @Property params unchanged>)` — `postCallback(@Body body: String, request: HttpRequest<Any>): HttpResponse<String>` and `putCallback(...)` keep their exact names and parameter types (`CustomCallbackRoutes` binds them reflectively)
  - `CallbackWebController(<contextPath @Property unchanged>, listCallbacksPort: ListCallbacksPort, clearCallbacksPort: ClearCallbacksPort)`

- [ ] **Step 1: Write the failing tests**

`PersistenceFailureTest`: `@MicronautTest`, replacing each port with a failing lambda via
`@MockBean(SaveCallbackPort::class) fun failingSave() = SaveCallbackPort { SaveCallbackError.PersistenceError("boom").left() }`
(same for List and Clear). Use the injected `RequestSpecification`.

- `saving a callback that fails to persist returns 500` — POST `TEST_JSON_PAYLOAD` to `CALLBACK_LOGGER_API_URL` → `statusCode(500)`
- `a put callback that fails to persist returns 500` — PUT, same → 500
- `a custom route callback that fails to persist returns 500` — POST to `/api/custom1` → 500
- `the inbox page returns 500 when callbacks cannot be loaded` — GET `/callback-logger/` → 500
- `clear returns 500 when callbacks cannot be cleared` — GET `/callback-logger/clear` with `.redirects().follow(false)` → 500

In `CustomCallbackTests`, extend `a successful post request to the custom endpoint returns a 200 response` with
`expectThat(callbackRepository.findAll().toList()).hasSize(1)` after the request.

- [ ] **Step 2: Run them before touching production code**

Run: `./gradlew test --tests '*PersistenceFailureTest' --tests '*CustomCallbackTests'`
Expected: PASS. These are characterisation tests: Task 1's bridge throws on `Left` and Micronaut answers 500, which
is today's behaviour. They pin it so Step 3's rewrite can't silently turn a failure into a 200 or a redirect.

- [ ] **Step 3: Move the web classes, rename the DTO, inject the ports, delete `CallbackService`**

Each controller folds the port's `Either`: `Left` → `logger.error("Failed to save callback: {}", error)` (or
`list callbacks` / `clear callbacks`) then `HttpResponse.serverError()`; `Right` → today's response. Logger is
`companion object { private val logger = LoggerFactory.getLogger(<Controller>::class.java) }`. The `println` dump
runs before the save, as now.

- [ ] **Step 4: Run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL; `PersistenceFailureTest` 5 passing; `src/main/.../callbacks/` and `src/test/.../callbacks/` no longer exist.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit --no-gpg-sign -m "Call callback ports directly from the web adapter"
```

---

### Task 3: Enforce the boundaries with ArchUnit

**Files:**
- Create: `src/test/kotlin/com/leeturner/callback_logger/ArchitectureTest.kt`

**Interfaces:**
- Consumes: the package layout from Tasks 1–2.

- [ ] **Step 1: Write the test**

Copy `~/dev/encore/encore-web-api/src/test/kotlin/com/leeturner/encore/ArchitectureTest.kt` verbatim (three tests,
both import options), then replace `com.leeturner.encore` with `com.leeturner.callback_logger` and the adapter list
with exactly:

```kotlin
.adapter("web", "com.leeturner.callback_logger.adapters.inbound.web..")
.adapter("db", "com.leeturner.callback_logger.adapters.outbound.db..")
```

- [ ] **Step 2: Run it**

Run: `./gradlew test --tests '*ArchitectureTest'`
Expected: 3 passing. A failure means a real boundary leak from Tasks 1–2: fix the production code, not the rule.

- [ ] **Step 3: Prove it bites**

Temporarily add `import io.micronaut.core.annotation.Introspected` and `@Introspected` to `domain/callbacks/Callback.kt`,
rerun Step 2's command, expect `the domain is free of frameworks` to FAIL, then revert the change.

- [ ] **Step 4: Run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/test/kotlin/com/leeturner/callback_logger/ArchitectureTest.kt
git commit --no-gpg-sign -m "Enforce ports and adaptors boundaries with ArchUnit"
```
