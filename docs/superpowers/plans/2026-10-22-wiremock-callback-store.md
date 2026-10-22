# WireMock Callback Store Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Receive callbacks on any method and path through an embedded WireMock server on its own port, and make
its request journal the store the inbox reads and clears.

**Architecture:** A new outbound adapter, `adapters/outbound/wiremock`, owns a started `WireMockServer` bean (Jetty,
one catch-all stub, admin API locked) and implements `ListCallbacksPort` / `ClearCallbacksPort` over its journal.
The Micronaut receive side (API controller, custom routes, save port) and the H2 adapter are deleted. The web
adapter is unchanged apart from the empty-inbox URL.

**Tech Stack:** Kotlin 2.4, Micronaut 5.2.2 (Netty, Thymeleaf), WireMock 4.0.0-beta.41 (Jetty 12), Arrow core,
Strikt, ArchUnit, JUnit 6, RestAssured.

**Spec:** `docs/superpowers/specs/2026-10-22-wiremock-callback-store-design.md`

## Global Constraints

- WireMock deps, inline in `build.gradle.kts`, all `4.0.0-beta.41`: `org.wiremock:wiremock-core`,
  `org.wiremock:wiremock-jetty`, `org.wiremock:wiremock-httpclient-apache5` (required, or startup fails with
  `FatalStartupException: No suitable HttpClientFactory was found`).
- Stay on `micronautVersion=5.2.2`.
- Config: `callback-logger.wiremock.port` (default `7071`, tests `0` = random); `callback-logger.callback-response-status-code`
  and `callback-logger.callback-response-payload` keep their names and meaning.
- Every callback response: configured status, configured body, `Content-Type: application/json`.
- Admin API locked: every `/__admin/**` request answers 401.
- Nothing under `com.leeturner.callback_logger.domain` imports WireMock, `io.micronaut..` or `jakarta..`.
- The web adapter never touches `WireMockServer`; it goes through the ports.
- Adapters keep `either { catch({ … }) { e: Exception -> raise(PersistenceError(e.message ?: "Unknown persistence error")) } }`.
- Commit with `git commit --no-gpg-sign`, message ending `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Suite: `./gradlew test` green at the end of every task (baseline: green).

## Review Focus

1. A binary (non-UTF-8) callback body must not break the inbox page: it still renders 200. (Task 3)
2. A callback with no body (e.g. a `GET`) shows "No body" in the inbox, not an empty `<pre>`. (Task 3)
3. A long payload (10,000 chars) comes back whole. H2 needed a CLOB for this; check the journal doesn't truncate it. (Task 3)
4. A request under `/__admin` is refused and is not recorded as a callback. (Task 1)
5. A query string survives into the inbox: `uri` shows path and query, e.g. `/hook?a=1&b=2`. (Task 3)

---

### Task 1: WireMock receives callbacks

The old Micronaut endpoint keeps working during this task; WireMock runs alongside it.

**Files:**
- Modify: `build.gradle.kts` (add the three WireMock deps)
- Modify: `src/main/resources/application.conf` (add `callback-logger.wiremock.port = 7071`)
- Modify: `src/test/resources/application-test.conf` (add `callback-logger.wiremock.port = 0`)
- Create: `src/main/kotlin/com/leeturner/callback_logger/adapters/outbound/wiremock/WireMockFactory.kt`
- Modify: `src/test/kotlin/com/leeturner/callback_logger/TestFixtures.kt` (add `sendCallback`)
- Modify: `src/test/kotlin/com/leeturner/callback_logger/ArchitectureTest.kt`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/outbound/wiremock/CallbackReceiptTest.kt`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/outbound/wiremock/WireMockResponseStatusTest.kt`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/outbound/wiremock/WireMockResponsePayloadTest.kt`

**Interfaces:**
- Produces: a `@Context` singleton `com.github.tomakehurst.wiremock.WireMockServer` bean, started, stopped on
  context close (`@Bean(preDestroy = "stop")`).
- Produces (test): `TestFixtures.sendCallback(server: WireMockServer, path: String, body: String = "", contentType: String = MediaType.APPLICATION_JSON, method: String = "POST"): ValidatableResponse`
  — RestAssured `given().baseUri("http://localhost").port(server.port())…request(method, path).then()`.

- [ ] **Step 1: Write the failing tests**

`CallbackReceiptTest` (`@MicronautTest`, inject `WireMockServer`, `@BeforeEach server.resetRequests()`):

```kotlin
@ParameterizedTest
@ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "PROPFIND"])
fun `a callback on any method and nested path gets the configured response`(method: String) {
  sendCallback(server, "/any/nested/path?a=1", TEST_JSON_PAYLOAD, method = method)
      .statusCode(200).contentType("application/json").body(equalTo(""))
  expectThat(server.allServeEvents.single().request) {
    get { this.method.name } isEqualTo method
    get { url } isEqualTo "/any/nested/path?a=1"
  }
}

@ParameterizedTest
@CsvSource(delimiter = ';', value = ["${MediaType.APPLICATION_JSON};${TEST_JSON_PAYLOAD}", "${MediaType.APPLICATION_XML};${TEST_XML_PAYLOAD}"])
fun `a callback body is recorded whatever its media type`(mediaType: String, payload: String) {
  sendCallback(server, "/callback", payload, mediaType).statusCode(200)
  expectThat(server.allServeEvents.single().request.bodyAsString) isEqualTo payload
}

@Test
fun `the admin api is refused and nothing is recorded`() {
  sendCallback(server, "/__admin/mappings", method = "GET").statusCode(401)
  expectThat(server.allServeEvents).isEmpty()
}

@Test
fun `shutdown through the admin api is refused and callbacks keep arriving`() {
  sendCallback(server, "/__admin/shutdown").statusCode(401)
  sendCallback(server, "/callback", TEST_JSON_PAYLOAD).statusCode(200)
}
```

`WireMockResponseStatusTest` (`@MicronautTest(environments = ["status"])`): POST and PUT to `/callback` →
`statusCode(201)`. `WireMockResponsePayloadTest` (`@MicronautTest(environments = ["payload"])`): POST and PUT to
`/callback` → `statusCode(200)`, `body(equalTo("""{"status": "OK", "message": "Callback received"}"""))`.
Both reuse the existing `application-status.conf` / `application-payload.conf`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests '*.adapters.outbound.wiremock.*'`
Expected: FAIL — no bean of type `WireMockServer`.

- [ ] **Step 3: Add the deps and implement `WireMockFactory`**

`@Factory class WireMockFactory` with
`@Context @Bean(preDestroy = "stop") fun wireMockServer(@Property(name = "callback-logger.wiremock.port") port: Int, @Property(name = "callback-logger.callback-response-status-code") status: HttpStatus, @Property(name = "callback-logger.callback-response-payload") payload: String): WireMockServer`.
Config: `wireMockConfig().port(port).notifier(ConsoleNotifier(true)).adminAuthenticator { false }`; `start()`, then
`stubFor(any(anyUrl()).willReturn(aResponse().withStatus(status.code).withHeader("Content-Type", "application/json").withBody(payload)))`.
Add the config keys to both `.conf` files. In `ArchitectureTest`, add
`.adapter("wiremock", "com.leeturner.callback_logger.adapters.outbound.wiremock..")` (keep `db` for now).

- [ ] **Step 4: Run the full suite**

Run: `./gradlew test`
Expected: PASS, including the new tests and the old `CallbackApi*` tests.

- [ ] **Step 5: Commit** — `Receive callbacks on any method and path with an embedded WireMock server`

---

### Task 2: Remove the Micronaut receive side

**Files:**
- Delete: `adapters/inbound/web/CallbackApiController.kt`, `CustomCallbackRoutes.kt`, `CustomCallbackConfiguration.kt`
- Delete: `domain/callbacks/NewCallback.kt`, `SaveCallbackError.kt`, `ports/SaveCallbackPort.kt`
- Delete: `adapters/outbound/db/DatabaseCallbackSaverAdapter.kt`
- Delete tests: `CallbackApiControllerTest`, `CallbackApiResponseStatusTests`, `CallbackApiResponsePayloadTests`, `CustomCallbackTests`
- Modify: `PersistenceFailureTest` (drop `failingSave` and the three save tests)
- Modify: `DatabaseCallbackAdaptersTest` (drop the four `save …` tests; seed the remaining two through `DbCallbackRepository.save(DbCallback(...))`)
- Modify: `TestFixtures.kt` (drop `CALLBACK_LOGGER_API_URL`)
- Modify: `application.conf`, `application-test.conf` (drop `custom-callbacks`)

**Interfaces:**
- Consumes: Task 1's `WireMockServer` bean, now the only reader of `callback-response-*`.

- [ ] **Step 1: Delete the files and test cases listed above**

- [ ] **Step 2: Check nothing still refers to them**

Run: `grep -rn "SaveCallbackPort\|NewCallback\|CustomCallback\|CallbackApiController\|custom-callbacks\|CALLBACK_LOGGER_API_URL" src`
Expected: no output.

- [ ] **Step 3: Run the full suite**

Run: `./gradlew test`
Expected: PASS.

- [ ] **Step 4: Commit** — `Remove the Micronaut callback endpoint now WireMock receives callbacks`

---

### Task 3: The inbox reads WireMock's journal

**Files:**
- Modify: `domain/callbacks/Callback.kt` → `data class Callback(val id: String, val timestamp: LocalDateTime, val uri: String, val httpMethod: String, val httpVersion: String, val payload: String)`
- Delete: `domain/callbacks/CallbackStatus.kt`
- Modify: `adapters/inbound/web/CallbackView.kt` (`id: String`, drop `status`)
- Create: `adapters/outbound/wiremock/WireMockCallbackListerAdapter.kt`, `WireMockCallbackClearerAdapter.kt`, `ServeEventMapper.kt`
- Delete: `adapters/outbound/db/` (all), test `adapters/outbound/db/DatabaseCallbackAdaptersTest.kt`
- Modify: `build.gradle.kts` (remove `micronaut-data-jdbc`, `micronaut-jdbc-hikari`, `h2`)
- Modify: `application.conf` (remove `datasources`)
- Modify: `ArchitectureTest.kt` (remove the `db` adapter)
- Modify: `TestFixtures.kt` (remove the `DbCallback` fixtures)
- Modify: `CallbackWebListViewMultipleCallbacksTest.kt`, `PersistenceFailureTest.kt` imports if needed
- Test: `adapters/outbound/wiremock/WireMockCallbackAdaptersTest.kt`

**Interfaces:**
- Consumes: the `WireMockServer` bean (Task 1), `TestFixtures.sendCallback` (Task 1).
- Produces: `@Singleton class WireMockCallbackListerAdapter(server: WireMockServer) : ListCallbacksPort`,
  `@Singleton class WireMockCallbackClearerAdapter(server: WireMockServer) : ClearCallbacksPort`,
  `internal fun ServeEvent.toDomain(): Callback` — mapping exactly as the spec's table
  (`id.toString()`, `LocalDateTime.ofInstant(request.loggedDate.toInstant(), ZoneId.systemDefault())`,
  `request.url`, `request.method.name`, `request.protocol`, `request.bodyAsString`).

- [ ] **Step 1: Write the failing adapter test**

`WireMockCallbackAdaptersTest` (`@MicronautTest`, inject `WireMockServer`, lister, clearer; `@BeforeEach server.resetRequests()`):

```kotlin
@Test
fun `a received callback is listed with its fields`() {
  sendCallback(server, "/api/custom-callback?a=1&b=2", TEST_XML_PAYLOAD, MediaType.APPLICATION_XML, "PUT")
  expectThat(lister.listNewestFirst()).isRight().get { value.single() }.and {
    get { id }.isNotBlank()
    get { uri } isEqualTo "/api/custom-callback?a=1&b=2"
    get { httpMethod } isEqualTo "PUT"
    get { httpVersion } isEqualTo "HTTP/1.1"
    get { payload } isEqualTo TEST_XML_PAYLOAD
    get { timestamp } isGreaterThan LocalDateTime.now().minusMinutes(1)
  }
}

@Test
fun `listNewestFirst returns the most recent first`() {
  sendCallback(server, "/first"); sendCallback(server, "/second")
  expectThat(lister.listNewestFirst()).isRight().get { value.map(Callback::uri) }.containsExactly("/second", "/first")
}

@Test
fun `a long payload is listed whole`() {
  val payload = "x".repeat(10_000)
  sendCallback(server, "/long", payload, MediaType.TEXT_PLAIN)
  expectThat(lister.listNewestFirst()).isRight().get { value.single().payload } isEqualTo payload
}

@Test
fun `clear removes every callback and keeps receiving`() {
  sendCallback(server, "/one"); sendCallback(server, "/two")
  expectThat(clearer.clear()).isRight()
  expectThat(lister.listNewestFirst()).isRight().get { value }.isEmpty()
  sendCallback(server, "/three").statusCode(200)
  expectThat(lister.listNewestFirst()).isRight().get { value }.hasSize(1)
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests '*WireMockCallbackAdaptersTest'`
Expected: FAIL — compilation, `WireMockCallbackListerAdapter` not defined.

- [ ] **Step 3: Implement the adapters and mapper, change `Callback`/`CallbackView`, delete `db` and H2**

The lister returns `server.allServeEvents.map { it.toDomain() }` in journal order (WireMock already lists newest
first). The clearer calls `server.resetRequests()`.

- [ ] **Step 4: Re-seed the web tests through WireMock and add the Review Focus cases**

`CallbackWebListViewMultipleCallbacksTest`: inject `WireMockServer`; `@BeforeEach` = `server.resetRequests()`,
`sendCallback(server, "/api/callback", TEST_JSON_PAYLOAD)`, then
`sendCallback(server, "/api/custom-callback", TEST_XML_PAYLOAD, MediaType.APPLICATION_XML)`. The ordering test keeps
"XML first", with `httpVersion` now `"HTTP/1.1"`. The clear test asserts `server.allServeEvents` is empty instead of
`callbackRepository.count()`. Add to the same class:

```kotlin
@Test
fun `a callback without a body shows No body`(spec: RequestSpecification) {
  server.resetRequests(); sendCallback(server, "/ping", method = "GET")
  // GET /callback-logger/ → 200; callback-1-payload text is "No body"
}

@Test
fun `a callback with a binary body still renders the inbox`(spec: RequestSpecification) {
  server.resetRequests()
  // RestAssured directly, as sendCallback takes a String: invalid UTF-8 bytes must reach WireMock as-is
  given().baseUri("http://localhost").port(server.port()).contentType("application/octet-stream")
      .body(byteArrayOf(0xC3.toByte(), 0x28, 0x00, 0xFF.toByte())).post("/bin").then().statusCode(200)
  // GET /callback-logger/ → 200, contentType HTML, callback-count is 1
}

@Test
fun `a callback uri keeps its query string`(spec: RequestSpecification) {
  server.resetRequests(); sendCallback(server, "/hook?a=1&b=2")
  // GET /callback-logger/ → callback-1-uri text is "/hook?a=1&b=2"
}
```

- [ ] **Step 5: Run the full suite and check H2 is gone**

Run: `./gradlew test && grep -rn "h2\|micronaut-data\|DbCallback\|CallbackStatus\|datasources" build.gradle.kts src`
Expected: tests PASS; grep prints nothing.

- [ ] **Step 6: Commit** — `Read and clear callbacks from WireMock's request journal instead of H2`

---

### Task 4: Point people at the new callback URL

**Files:**
- Modify: `adapters/inbound/web/CallbackWebController.kt` (inject `@Property(name = "callback-logger.wiremock.port") wireMockPort: Int`; model gains `"callbackUrl" to "http://localhost:$wireMockPort/callback"`)
- Modify: `src/main/resources/views/inbox.html` (empty-state `curl` uses `${callbackUrl}`; the `uri` prototype text becomes `/callback`)
- Modify: `CallbackWebListViewZeroCallbacksTest.kt`
- Modify: `README.md`, `src/test/resources/http/callbacks.http`, `build.gradle.kts` (dockerfile block)

- [ ] **Step 1: Write the failing test** in `CallbackWebListViewZeroCallbacksTest`, taking the port as a constructor
  parameter `@Property(name = "callback-logger.wiremock.port") private val port: Int` (the pattern
  `CallbackApiResponsePayloadTests` uses today):

```kotlin
@Test
fun `the empty inbox shows the wiremock callback url`(spec: RequestSpecification) {
  // GET /callback-logger/ → text of the <pre> inside #empty-inbox contains "http://localhost:$port/callback"
  //                         and does not contain "/callback-logger/api/callback"
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests '*CallbackWebListViewZeroCallbacksTest'`
Expected: FAIL — the `<pre>` still has `http://localhost:7070/callback-logger/api/callback`.

- [ ] **Step 3: Implement the controller and template change**

- [ ] **Step 4: Update the docs and Docker**

- `README.md`: callbacks go to `http://localhost:7071/<any path>` with any HTTP method; a **Breaking change** note
  (was `localhost:7070/callback-logger/api/callback`); `callback-logger.wiremock.port`; remove *Custom callbacks*;
  status/payload sections keep their keys (also fix the existing `custom-callback-response-code` typo to
  `callback-response-status-code`); `/__admin` paths are reserved (401, not logged); JVM only, no native image.
- `callbacks.http`: both requests to `http://localhost:7071/callback`.
- `build.gradle.kts` dockerfile block: `exportPorts(7070, 7071)`.

- [ ] **Step 5: Verify the build, the Dockerfile and the fat jar**

Run: `./gradlew build dockerfile shadowJar && grep EXPOSE build/docker/main/Dockerfile`
Expected: BUILD SUCCESSFUL; `EXPOSE 7070 7071`.

Then run `java -jar build/libs/callback-logger-0.1-all.jar` (background) and:
`curl -s -o /dev/null -w '%{http_code}' -X PATCH localhost:7071/smoke/test -d '{"a":1}'` → `200`;
`curl -s localhost:7070/callback-logger/inbox | grep -c '/smoke/test'` → `1`;
`curl -s -o /dev/null -w '%{http_code}' localhost:7071/__admin/mappings` → `401`. Stop the jar.

- [ ] **Step 6: Commit** — `Point the inbox, README and Dockerfile at the WireMock callback port`
