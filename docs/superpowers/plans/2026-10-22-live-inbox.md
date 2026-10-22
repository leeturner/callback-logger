# Live Inbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Push each change to the callbacks into the open inbox page over server-sent events, and drop the 5-second
auto-refresh.

**Architecture:** A new domain port, `WatchCallbacksPort`, lets the web adapter hear about changes. The WireMock
adapter implements it as a `ServeEventListener` whose `afterComplete` fires once the request is in the journal. The
web controller fans changes out to every open `GET /inbox/stream` through a Reactor sink, renders the existing
`inbox` template for each one and sends it as a Datastar `datastar-patch-elements` event.

**Tech Stack:** Kotlin 2.4, Micronaut 5.2 (Netty, Thymeleaf, SSE via `io.micronaut.http.sse.Event`), Reactor,
WireMock 4.0.0-beta.41, Datastar 1.0.4, Arrow, Strikt, ArchUnit, JUnit 6, RestAssured, `java.net.http.HttpClient`.

**Spec:** `docs/superpowers/specs/2026-10-22-live-inbox-design.md`

## Global Constraints

- Nothing under `com.leeturner.callback_logger.domain` imports WireMock, Micronaut, Reactor or `jakarta..`
  (`ArchitectureTest` checks it).
- The web adapter never touches `WireMockServer`; it goes through the ports.
- Reactor is on the runtime classpath but not the compile classpath. Add `implementation("io.micronaut.reactor:micronaut-reactor")`
  (version from the Micronaut BOM, no version string). No other new dependency.
- No `micronaut-http-client` on the test classpath: stream tests use `java.net.http.HttpClient`.
- SSE event: name `datastar-patch-elements`, data `"mode replace\n" + html.lines().joinToString("\n") { "elements $it" }`.
  Micronaut's `TextStreamBodyWriter` splits data on `\n` into `data: ` lines, so every HTML line must carry its own
  `elements ` prefix. `String.lines()` also splits on `\r`, which SSE treats as a line end too.
- Refresh button stays. Auto-refresh checkbox, the `autoRefresh` signal and the `data-on-interval` div go.
- Commit with `git commit --no-gpg-sign`, message ending `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `./gradlew test` green at the end of every task (baseline: green).

## Review Focus

1. **A multi-line payload, including `\r\n`, patches whole.** Every `data:` line is `mode replace` or starts with
   `elements `. Otherwise Datastar drops everything after the first line. (Task 2)
2. **A tab left open on a quiet inbox keeps streaming.** Netty closes a connection after 5 minutes with no reads or
   writes (`ALL_IDLE`, `idle-timeout`). A 30-second comment-only heartbeat keeps it open. Datastar ignores events
   whose name doesn't start with `datastar`. (Task 2, plus a manual check in Task 3)
3. **A burst of callbacks doesn't queue one render per callback per tab.** `onBackpressureLatest()` before a
   prefetch-1 `publishOn` folds a burst into the latest change. (Task 2)
4. **The last tab closing doesn't kill the sink for the next tab.** Use `Sinks.many().multicast().directBestEffort()`.
   `onBackpressureBuffer()` auto-cancels when the last subscriber leaves. (Task 2 test opens streams in several
   tests on one context)
5. **An `/__admin` request (401) doesn't push.** It goes through WireMock's admin handler, not the stub handler.
   (Task 1)

---

### Task 1: WireMock tells listeners when a callback arrives

**Files:**
- Create: `src/main/kotlin/com/leeturner/callback_logger/domain/callbacks/ports/WatchCallbacksPort.kt`
- Create: `src/main/kotlin/com/leeturner/callback_logger/adapters/outbound/wiremock/WireMockCallbackWatcherAdapter.kt`
- Modify: `src/main/kotlin/com/leeturner/callback_logger/adapters/outbound/wiremock/WireMockFactory.kt`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/outbound/wiremock/WireMockCallbackAdaptersTest.kt`

**Interfaces:**
- Produces: `fun interface WatchCallbacksPort { fun onChange(listener: () -> Unit) }`. Its KDoc says the listener
  is called after each new callback is stored, on the receiving thread.
- Produces: `@Singleton class WireMockCallbackWatcherAdapter : WatchCallbacksPort, ServeEventListener`.

- [ ] **Step 1: Write the failing tests** in `WireMockCallbackAdaptersTest`. Add a `watcher: WireMockCallbackWatcherAdapter`
  constructor parameter.

```kotlin
@Test
internal fun `a listener hears about a callback once it is in the journal`() {
  val journalSizes = LinkedBlockingQueue<Int>()
  watcher.onChange { journalSizes.add(server.allServeEvents.size) }

  sendCallback(server, "/callback", TEST_JSON_PAYLOAD).statusCode(200)

  expectThat(journalSizes.poll(5, SECONDS)) isEqualTo 1
}

@Test
internal fun `an admin request does not notify listeners`() {
  val notifications = LinkedBlockingQueue<Unit>()
  watcher.onChange { notifications.add(Unit) }

  sendCallback(server, "/__admin/mappings", method = "GET").statusCode(401)
  sendCallback(server, "/callback", TEST_JSON_PAYLOAD).statusCode(200)

  expectThat(notifications.poll(5, SECONDS)).isNotNull()
  expectThat(notifications.poll(500, MILLISECONDS)).isNull()
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew test --tests '*WireMockCallbackAdaptersTest*'`
Expected: compilation fails, `Unresolved reference: WireMockCallbackWatcherAdapter`.

- [ ] **Step 3: Implement the port and the adapter.** Keep listeners in a `CopyOnWriteArrayList<() -> Unit>`.
  `afterComplete(serveEvent, parameters)` calls each one. `getName()` returns `"callback-watcher"`. Add a comment
  saying why it is `afterComplete` and not `addMockServiceRequestListener`: the request listener fires before the
  request is written to the journal.

- [ ] **Step 4: Register it.** Give `WireMockFactory.wireMockServer` a `watcher: WireMockCallbackWatcherAdapter`
  parameter and add `.extensions(watcher)` to the `wireMockConfig()` chain.

- [ ] **Step 5: Run the whole suite**

Run: `./gradlew test`
Expected: PASS, including `ArchitectureTest`.

- [ ] **Step 6: Commit** — "Tell listeners when WireMock records a callback"

---

### Task 2: Stream the inbox to the page

**Files:**
- Modify: `build.gradle.kts` (add `implementation("io.micronaut.reactor:micronaut-reactor")`)
- Modify: `src/main/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CallbackWebController.kt`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/inbound/web/InboxStreamTest.kt`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/inbound/web/PersistenceFailureTest.kt`

**Interfaces:**
- Consumes: `WatchCallbacksPort.onChange(listener: () -> Unit)` from Task 1.
- Produces: `GET /callback-logger/inbox/stream`, `text/event-stream`. Task 3's page opens it.

- [ ] **Step 1: Write `InboxStreamTest`** (`@MicronautTest`). Inject `EmbeddedServer`, `WireMockServer` and
  `RequestSpecification`, and call `server.resetRequests()` in `@BeforeEach`. Add a private helper
  `openStream(): BlockingQueue<String>`. It sends `GET ${embeddedServer.uri}/callback-logger/inbox/stream` with
  `HttpClient.send(..., BodyHandlers.ofLines())`, then reads the lines on a daemon thread. Lines are joined into
  events (a blank line ends one), and only events containing `event: datastar-patch-elements` are queued, so
  heartbeats are skipped. Keep the `Stream<String>` and close it in `@AfterEach`. `nextEvent(queue)` is
  `queue.poll(5, SECONDS) ?: fail("no event")`.

```kotlin
@Test
internal fun `the stream sends the current inbox as soon as it opens`() {
  val event = nextEvent(openStream())

  expectThat(event) {
    contains("data: mode replace")
    contains("""id="callback-count"""")
    contains("""id="empty-inbox"""")
  }
}

@Test
internal fun `a callback that arrives is pushed down the stream`() {
  val events = openStream()
  nextEvent(events)

  sendCallback(wireMock, "/live-test", TEST_JSON_PAYLOAD).statusCode(200)

  expectThat(nextEvent(events)).contains("/live-test")
}

@Test
internal fun `clearing the inbox is pushed down the stream`() {
  sendCallback(wireMock, "/live-test", TEST_JSON_PAYLOAD).statusCode(200)
  val events = openStream()
  expectThat(nextEvent(events)).contains("/live-test")

  spec.`when`().delete("/callback-logger/inbox").then().statusCode(200)

  expectThat(nextEvent(events)).contains("""id="empty-inbox"""")
}

@Test
internal fun `every line of a multi-line payload is sent as an elements line`() {
  val events = openStream()
  nextEvent(events)

  sendCallback(wireMock, "/multi-line", "{\r\n  \"a\": 1,\n  \"b\": 2\n}").statusCode(200)

  val dataLines = nextEvent(events).lines().filter { it.startsWith("data:") }
  expectThat(dataLines.first()) isEqualTo "data: mode replace"
  expectThat(dataLines.drop(1)).all { startsWith("data: elements ") }
  expectThat(dataLines.joinToString("\n")).contains("&quot;b&quot;: 2")
}
```

- [ ] **Step 2: Add to `PersistenceFailureTest`.** The test opens the stream with `HttpClient` and
  `BodyHandlers.ofString()` and a 5-second request timeout, and expects status `500`. Name it
  `` `the inbox stream returns 500 when callbacks cannot be loaded` ``. The test class's `@MockBean`s need a
  `WatchCallbacksPort { }` no-op as well, if the context needs it to start. On a 500, Datastar doesn't retry (with
  `retry: "auto"` it only retries network errors) and it shows the existing error banner.

- [ ] **Step 3: Run them to see them fail**

Run: `./gradlew test --tests '*InboxStreamTest*' --tests '*PersistenceFailureTest*'`
Expected: FAIL. The stream answers 404.

- [ ] **Step 4: Implement the stream in `CallbackWebController`.**
  - The constructor takes `watchCallbacksPort: WatchCallbacksPort` and `viewsRenderer: ViewsRenderer<Map<String, Any>, HttpRequest<*>>`.
  - `private val changes = Sinks.many().multicast().directBestEffort<Unit>()`, registered in `init` with
    `watchCallbacksPort.onChange(::changed)`.
  - `@Synchronized private fun changed() { changes.tryEmitNext(Unit) }`. Emits arrive from Jetty and Netty
    threads, and unsynchronised `tryEmitNext` fails with `FAIL_NON_SERIALIZED`.
  - `clear` and `delete` call `changed()` on success, before returning `inboxResponse()`, so other tabs update.
  - `@Get("/inbox/stream", produces = [MediaType.TEXT_EVENT_STREAM]) fun stream(request: HttpRequest<*>): Publisher<Event<String>>`:

```kotlin
// starts with Unit so a (re)connecting page gets the current inbox straight away
Flux.concat(Flux.just(Unit), changes.asFlux())
    // a burst of callbacks becomes one render of the latest inbox, off WireMock's thread
    .onBackpressureLatest()
    .publishOn(Schedulers.boundedElastic(), 1)
    .map { Event.of(patchElements(render(request))).name("datastar-patch-elements") }
    // Netty closes a connection after 5 minutes with no reads or writes
    .mergeWith(Flux.interval(HEARTBEAT).map { Event.of("").comment("keep-alive") })
```

  - `render(request)` passes `"inbox"` and `inboxModel()` to `viewsRenderer.render(...)` and writes the `Writable`
    into a `StringWriter`. `patchElements(html)` is the format in Global Constraints. `HEARTBEAT = Duration.ofSeconds(30)`.
  - If the initial render fails, the 500 test pins whatever status Micronaut gives. If it commits `200` before the
    first item, move the first render ahead of the `Flux` (render eagerly, then `Flux.just(event)`) so the
    `IllegalStateException` escapes as a 500.

- [ ] **Step 5: Run the whole suite**

Run: `./gradlew test`
Expected: PASS.

- [ ] **Step 6: Commit** — "Stream inbox changes to the page"

---

### Task 3: The page listens instead of polling

**Files:**
- Modify: `src/main/resources/views/callbacks.html`
- Modify: `src/main/resources/views/inbox.html` (empty-inbox subtitle)
- Modify: `README.md:107`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CallbackWebListViewZeroCallbacksTest.kt`

**Interfaces:**
- Consumes: `GET /callback-logger/inbox/stream` from Task 2.

- [ ] **Step 1: Write the failing tests** in `CallbackWebListViewZeroCallbacksTest`. Check the raw markup, as the
  refresh-button test does.

```kotlin
@Test
internal fun `the callback list view opens the live inbox stream`(spec: RequestSpecification) {
  val page = spec.`when`().get(CALLBACK_LOGGER_WEB_URL).then().statusCode(200).extract().asString()

  expectThat(page) {
    contains("""data-init="@get('/callback-logger/inbox/stream')"""")
    not().contains("auto-refresh")
    not().contains("data-on-interval")
  }
}
```

  Change the empty-inbox test to also check `p[1]` is `"Send a request and it will show up here."`.

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew test --tests '*CallbackWebListViewZeroCallbacksTest*'`
Expected: FAIL on both.

- [ ] **Step 3: Change the page.**
  - In `callbacks.html`, remove the Auto-refresh `<label>`, `autoRefresh: false` from the body's `data-signals`,
    and the `data-on-interval` div.
  - In the interval div's place, add `<div id="inbox-stream" data-init="@get('/callback-logger/inbox/stream')"></div>`.
    It must stay outside the patched fragments, or each patch re-runs `data-init` and reconnects.
  - In `inbox.html`, change the empty-inbox subtitle to `Send a request and it will show up here.`
  - In the README (line 107), say the inbox updates as callbacks arrive and Refresh reloads it on demand. Drop the
    Auto-refresh mention.

- [ ] **Step 4: Run the whole suite**

Run: `./gradlew test`
Expected: PASS.

- [ ] **Step 5: Check it in the real app.** Run `./gradlew run` and open `http://localhost:7070/callback-logger/`
  in two tabs.
  - A `curl` to `:7071/anything` appears in both tabs without a click.
  - Clear All in one tab empties the other.
  - Header panels opened in a card stay open when a new callback arrives.
  - Refresh still works.
  - Leave a tab quiet for over 5 minutes, send another callback, and it still appears without the error banner.

- [ ] **Step 6: Commit** — "Show new callbacks live instead of polling"
