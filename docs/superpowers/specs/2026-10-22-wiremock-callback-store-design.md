# WireMock callback store

## Intent

Callbacks are received by Micronaut on a fixed set of routes (`/callback-logger/api/callback` plus the configured
`custom-callbacks`) and stored in an in-memory H2 table that keeps only the URI, method, HTTP version, body and
timestamp. The goal is to let callbacks arrive on **any method and any path**, and to keep **far more about each
request** (headers, cookies, query and form parameters, client IP, the response sent) to display later.

WireMock already does both. An embedded `WireMockServer` listens on its own port with one catch-all stub, and its
request journal becomes the callback store. Micronaut keeps serving the inbox UI and reads the journal through the
existing ports.

## Goals

- Callbacks are received by an embedded WireMock server (Jetty) on its own configurable port, on any path and any
  HTTP method, including non-standard ones such as `PROPFIND`.
- The inbox lists and clears callbacks from WireMock's request journal through `ListCallbacksPort` and
  `ClearCallbacksPort`.
- H2, Micronaut Data, the callback API controller and the custom-callback routes are deleted.
- Ports and adaptors stays: WireMock is a new outbound adapter, enforced by `ArchitectureTest`.

## Non-goals

- Showing the extra request data in the inbox. This change makes it available (WireMock records it); a follow-up
  adds it to `Callback` and the page.
- Per-path responses from WireMock mapping files, response templating, or exposing more WireMock features.
- Capping the number of stored callbacks. The journal is unbounded, as the H2 table is today
  (`maxRequestJournalEntries(n)` is the one-line fix if memory becomes a problem).
- Keeping the old callback URL working. See *Breaking change*.

## Anti-goals

- Routing callbacks through Micronaut into WireMock (`DirectCallHttpServer`). The spike showed Micronaut cannot
  route custom methods, a root catch-all steals the static resources and `DELETE /inbox`, and the direct-call request
  loses cookies and form parameters. See *Approaches*.
- WireMock or Micronaut types under `domain`.
- The web adapter talking to `WireMockServer` directly instead of through the ports.

## Approaches

Spiked on 2026-10-22 against Micronaut 5.2.15 and WireMock 4.0.0-beta.40.

| | How | Trade-off |
|---|---|---|
| A. Direct call | Micronaut catch-all route (`DefaultRouteBuilder`, under a prefix) builds an `ImmutableRequest` and passes it to `DirectCallHttpServer.stubRequest()`. | One port, but: stacked `@Get/@Post/...` on one handler only registers GET; the route builder covers the standard methods only (custom ones get 405); a root catch-all broke `/static/**` and `DELETE /inbox`; programmatic routes ignore `context-path`; `ImmutableRequest` has no cookies, form parameters or multipart parts. |
| **B. WireMock on its own port (chosen)** | `WireMockServer` with Jetty on a second port and one `any(anyUrl())` stub; Micronaut only serves the UI. | Every method and path, cookies and form parameters recorded, no clash with UI routes. Costs a second port, the `wiremock-jetty` dependency, a breaking change to the callback URL, and locking down WireMock's admin API, which would otherwise be open on the callback port. |
| C. Keep H2, add columns | Store headers etc. in more columns, keep the Micronaut routes. | No new dependency, but keeps the fixed-route limitation and means hand-parsing what WireMock already parses. |

B was chosen because it gives both things asked for (any endpoint, more data) with the least code.

## Target layout

```
com.leeturner.callback_logger
├── Application.kt                                  (unchanged)
├── domain/callbacks/
│   ├── Callback.kt            id: String, timestamp, uri, httpMethod, httpVersion, payload
│   ├── ListCallbacksError.kt  (unchanged)
│   ├── ClearCallbacksError.kt (unchanged)
│   └── ports/
│       ├── ListCallbacksPort.kt   (unchanged)
│       └── ClearCallbacksPort.kt  (unchanged)
└── adapters/
    ├── inbound/web/
    │   ├── CallbackWebController.kt   (unchanged apart from the empty-inbox URL, below)
    │   └── CallbackView.kt            (id: String, status removed)
    └── outbound/wiremock/
        ├── WireMockFactory.kt                  @Factory: the configured, started WireMockServer bean
        ├── WireMockCallbackListerAdapter.kt    : ListCallbacksPort
        ├── WireMockCallbackClearerAdapter.kt   : ClearCallbacksPort
        └── ServeEvent.toDomain()               internal mapper
```

Deleted: `adapters/outbound/db/` (all of it), `CallbackApiController`, `CustomCallbackRoutes`,
`CustomCallbackConfiguration`, `NewCallback`, `SaveCallbackError`, `SaveCallbackPort`, `CallbackStatus`.

There is no inbound adapter and no save port: WireMock receives and records each callback itself, so no application
code runs when one arrives. The app only reads and clears the journal, which is why WireMock is an *outbound*
adapter.

`CallbackStatus` goes because nothing displays it and nothing ever sets it to `READ`.

## The WireMock server

`WireMockFactory` produces a single `WireMockServer` bean that is created eagerly at startup (`@Context`, so callbacks
are accepted before anyone opens the inbox) and stopped when the context closes. It is configured with:

- `port(callback-logger.wiremock.port)`; `0` means a random port (WireMock's `DYNAMIC_PORT`), which tests use.
- One stub, `any(anyUrl())`, returning `callback-logger.callback-response-status-code` with body
  `callback-logger.callback-response-payload`. These keys keep their current meaning. The status code is still bound as
  Micronaut's `HttpStatus`, so an invalid code still fails startup, as the README promises.
- The stub's response sends `Content-Type: application/json`, as today. Checked on 2026-10-22:
  `CallbackApiController` answers `content-type: application/json` whatever the request's content type, while a
  WireMock stub with no headers sends none. WireMock's extra `Matched-Stub-Id` response header is harmless and stays.
- The request journal on, at its default (unbounded).
- The admin API locked: `adminAuthenticator { false }`, so every `/__admin` request gets a 401 (decided). Left open,
  anyone who can send a callback could shut WireMock down (`POST /__admin/shutdown`) while the inbox still looks
  healthy, replace the catch-all stub, wipe the journal, or add proxy stubs that make outgoing requests from the
  host. The app reads and clears the journal through the `WireMockServer` object, not over HTTP, so it doesn't need
  the admin API.

## Mapping a ServeEvent to a Callback

| `Callback` | From `ServeEvent` | Note |
|---|---|---|
| `id` | `id.toString()` (UUID) | was `Long` |
| `timestamp` | `request.loggedDate` → `LocalDateTime` in the system zone | same type as today |
| `uri` | `request.url` (path and query) | |
| `httpMethod` | `request.method.name` | custom methods such as `PROPFIND` come through as-is |
| `httpVersion` | `request.protocol` | Jetty gives `HTTP/1.1`; today's value is `HTTP_1_1` |
| `payload` | `request.bodyAsString` | |

`ListCallbacksPort.listNewestFirst()` returns `wireMockServer.allServeEvents` mapped in order: the spike confirmed
WireMock returns them newest first. `ClearCallbacksPort.clear()` calls `wireMockServer.resetRequests()`, which clears
the journal but keeps the stub.

## Errors

The adapters keep the existing `either { catch({ … }) { e -> raise(PersistenceError(…)) } }` shape. The in-memory
journal is unlikely to fail, but the shape keeps the port contracts and the existing "a `Left` becomes a 500"
behaviour in `CallbackWebController` unchanged.

## Configuration

```hocon
callback-logger {
  callback-response-status-code = 200   // unchanged meaning, now drives the WireMock stub
  callback-response-payload = ""        // unchanged meaning, now drives the WireMock stub
  wiremock {
    port = 7071
  }
}
```

`custom-callbacks` and the whole `datasources` block are removed.

## Breaking change

Callbacks move from `http://localhost:7070/callback-logger/api/callback` to `http://localhost:7071/<any path>`.
The same path still works, but the port changes, so existing configs pointing at 7070 break. The README must say so
plainly, and also note that paths under `/__admin` are reserved: they are refused with a 401 and are not logged as
callbacks.

## Things that point at the old URL

- `inbox.html` empty-state `curl` example: show the WireMock URL built from the configured port (passed into the
  model by `CallbackWebController`), with an example path such as `/callback`.
- `README.md` usage, custom callbacks and response sections: rewrite for any path on the WireMock port; remove custom
  callbacks; note the breaking change.
- `src/test/resources/http/callbacks.http`.
- The generated Dockerfile (`tasks.named<MicronautDockerfile>("dockerfile")`) must also expose the WireMock port.

## Console output

`CallbackApiController` printed each callback to stdout. That goes with the controller; WireMock is created with
`notifier(ConsoleNotifier(true))`, so each received request is still printed, in WireMock's format.

## Dependencies

Inline in `build.gradle.kts`, the latest WireMock 4 beta:

- add `org.wiremock:wiremock-core`, `org.wiremock:wiremock-jetty` and `org.wiremock:wiremock-httpclient-apache5`,
  all `4.0.0-beta.41`. `httpclient-apache5` looks unused but is required: without an `HttpClientFactory` on the
  classpath WireMock 4 fails to start (`FatalStartupException: No suitable HttpClientFactory was found`).
- remove `micronaut-data-jdbc`, `micronaut-jdbc-hikari` and `h2`.

The spike ran Micronaut (Netty) and WireMock (Jetty 12) in one process without classpath conflicts.

## Architecture test

Replace the `db` adapter with `.adapter("wiremock", "com.leeturner.callback_logger.adapters.outbound.wiremock..")`.
The other two rules are unchanged.

## Tests

`application-test.conf` sets `callback-logger.wiremock.port = 0`; tests read the real port from the
`WireMockServer` bean and send callbacks to it with RestAssured.

- **Deleted:** `CustomCallbackTests`, `DatabaseCallbackAdaptersTest`, and the three save tests in
  `PersistenceFailureTest` (its list and clear tests stay).
- **Re-pointed at the WireMock port:** `CallbackApiControllerTest` (rename to fit, such as `CallbackReceiptTest`, moved
  to `adapters/outbound/wiremock`), `CallbackApiResponseStatusTests` and `CallbackApiResponsePayloadTests`.
  Receipt tests also cover what wasn't possible before: an arbitrary nested path, `PATCH`, `DELETE` and a custom
  method.
- **Admin lock:** a request to `/__admin/mappings` on the WireMock port gets a 401, and `POST /__admin/shutdown`
  gets a 401 and leaves the server accepting callbacks.
- **Seeding switches** in the web list-view tests and `TestFixtures`: from `DbCallbackRepository` to sending real
  callbacks to WireMock (or `wireMockServer.resetRequests()` to clear). The ordering test relies on send order instead
  of hand-set timestamps.
- **New:** `WireMockCallbackAdaptersTest`, covering newest-first ordering, clear, and the field mapping above.
- `./gradlew build` passes.
- Smoke check of the fat jar: run `build/libs/*-all.jar` and send one callback to the WireMock port. WireMock finds
  its HTTP client and Jetty server by service lookup, so a green test run doesn't prove the shadow jar starts.

## Calls made without asking (check these)

- The inbox doesn't show the extra data yet (non-goal); the domain `Callback` keeps today's fields minus `status`.
- Default WireMock port `7071`, next to the UI's `7070`.
- No backwards-compatible listener on 7070 for the old URL.
- The response status/payload config keys keep their names rather than moving under `wiremock`.
- Console output switches to WireMock's verbose notifier instead of a hand-written listener that matches the old
  `println` format.
- `httpVersion` shows `HTTP/1.1` instead of `HTTP_1_1`.
- WireMock `4.0.0-beta.41`, the latest beta (the spike and the playground used beta.40), rather than the stable 3.x line.
- JVM only: the GraalVM native image (`dockerfileNative`) is not supported after this change, because WireMock and
  Jetty would need a lot of reachability metadata. CI never built it, so nothing breaks there; the README says so.
- Build on the pinned `micronautVersion=5.2.2` (the spike ran on 5.2.15); upgrade only if something conflicts.

## Left to the builder

- File splits for the mapper and factory; KDoc on new adapters, following the existing ports.
- Exact test names and how receipt tests are grouped.
- Commit granularity.
