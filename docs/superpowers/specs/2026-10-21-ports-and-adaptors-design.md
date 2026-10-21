# Ports and adaptors migration

## Intent

callback-logger predates the ports-and-adaptors style used in Lee's newer apps
(`~/dev/encore/encore-web-api`, `~/dev/personal/micronaut-launch-tui`). The goal is the same structure and
boundaries here, so all the apps read the same way. **encore-web-api is the reference**: when in doubt, copy what it
does.

## Goals

- Restructure into `domain` / `adapters/{inbound,outbound}` with the domain free of Micronaut.
- One `fun interface` port per operation, implemented by small outbound adapters, as in encore.
- Boundaries enforced by an ArchUnit `ArchitectureTest`, as in encore.

## Non-goals

- No behaviour change: every endpoint, page, config key, status code, response body and the console `println`
  dump stays as it is. The existing integration tests are the safety net and must keep passing (moved/re-pointed,
  not weakened).
- No version catalog (deps stay inline in `build.gradle.kts`), no database change (H2, `create_drop`), no new
  features, no ktlint/formatting changes.

## Anti-goals

- A pass-through service layer. Controllers call ports directly (encore has no service layer; that's the point).
- Micronaut, Micronaut Data or serde annotations anywhere under `domain`.

## Target layout

```
com.leeturner.callback_logger
├── Application.kt                              (composition root, unchanged)
├── domain/callbacks/
│   ├── Callback.kt          data class: id: Long, timestamp, status, uri, httpMethod, httpVersion, payload
│   ├── NewCallback.kt       data class: uri, httpMethod, httpVersion, payload
│   ├── CallbackStatus.kt    enum NEW, READ (moved as-is)
│   ├── SaveCallbackError.kt / ListCallbacksError.kt / ClearCallbacksError.kt
│   │                        sealed interface, each with `data class PersistenceError(val message: String)`
│   └── ports/
│       ├── SaveCallbackPort.kt     fun save(newCallback: NewCallback): Either<SaveCallbackError, Callback>
│       ├── ListCallbacksPort.kt    fun listNewestFirst(): Either<ListCallbacksError, List<Callback>>
│       └── ClearCallbacksPort.kt   fun clear(): Either<ClearCallbacksError, Unit>
└── adapters/
    ├── inbound/web/
    │   ├── CallbackApiController.kt        (moved; calls SaveCallbackPort)
    │   ├── CallbackWebController.kt        (moved; calls ListCallbacksPort / ClearCallbacksPort)
    │   ├── CustomCallbackRoutes.kt         (moved)
    │   ├── CustomCallbackConfiguration.kt  (moved)
    │   └── CallbackView.kt                 (was CallbackViewDto, plus a Callback.toView() mapper)
    └── outbound/db/
        ├── DbCallback.kt                   (the old entity: @MappedEntity("callback"), defaults, CLOB payload)
        ├── DbCallbackRepository.kt         (the old CallbackRepository)
        ├── DatabaseCallbackSaverAdapter.kt   : SaveCallbackPort
        ├── DatabaseCallbackListerAdapter.kt  : ListCallbacksPort
        ├── DatabaseCallbackClearerAdapter.kt : ClearCallbacksPort
        └── DbCallback.toDomain()           (internal mapper, in one of the above or its own file)
```

`CallbackService` is deleted.

## Behaviour details that must survive

- `CustomCallbackRoutes` binds routes to `CallbackApiController` by method name and parameter types
  (`"postCallback"`/`"putCallback"`, `String`, `HttpRequest`). Those signatures must not change.
- `callbacks.html` reads `timestamp`, `formattedTimestamp`, `uri`, `httpMethod`, `httpVersion`, `payload`, `status`, `id`
  from each item and `callbackCount` from the model. `CallbackView` keeps all of those properties and stays
  `@Serdeable`.
- `DbCallback` keeps the defaults the old entity had (`timestamp = now()`, `status = NEW`), so a saved callback
  still gets them. The table name stays `callback`.
- `/clear` still redirects to `/$contextPath/`.

## Errors

Adapters wrap repository calls in Arrow `either { catch({ … }) { e -> raise(PersistenceError(e.message ?: "Unknown persistence error")) } }`,
exactly like `DatabaseUserFinderAdapter` in encore.

A `Left` from any port becomes an HTTP 500 (decided: same as today, when the exception bubbled to Micronaut). How the
controller produces the 500 (`HttpResponse.serverError()`, or throwing) is the builder's call; the body need not
match Micronaut's default error JSON.

## Dependencies

Add, inline in `build.gradle.kts`, matching encore's versions:

- `implementation("io.arrow-kt:arrow-core:2.2.3")`
- `testImplementation("io.strikt:strikt-arrow:0.35.1")`
- `testImplementation("com.tngtech.archunit:archunit:1.5.1")`

## Architecture test

`src/test/kotlin/com/leeturner/callback_logger/ArchitectureTest.kt`, a copy of encore's with the package swapped:

- onion rule: domain models `com.leeturner.callback_logger.domain.*`, domain services
  `com.leeturner.callback_logger.domain.*.ports..`, adapters `web` (`adapters.inbound.web..`) and `db`
  (`adapters.outbound.db..`), `withOptionalLayers(true)`, root package ignored as composition root.
- the domain only depends on `domain..`, `kotlin..`, `java..`, `arrow..`, `org.jetbrains.annotations..`.
- nothing depends on the root package.
- keep encore's import option excluding `/build/generated/ksp/test/`.

## Tests

- Tests move to packages mirroring the code: controller tests to `adapters/inbound/web`, `CallbackRepositoryTest`
  to `adapters/outbound/db` (testing the adapters through their ports and/or `DbCallbackRepository`).
- Tests that seed or clear data via `CallbackRepository` switch to `DbCallbackRepository`; `TestFixtures` builds
  `DbCallback`s.
- New unit tests: none required beyond the ArchitectureTest and adapter tests replacing `CallbackRepositoryTest`.
  The controllers stay covered by the existing integration tests.
- The full suite (`./gradlew test`) passes before and after.

## Calls made without asking (check these)

- Per-operation error types (`SaveCallbackError` etc.) rather than one shared error, mirroring encore.
- `SaveCallbackPort` returns the saved `Callback` (the controller ignores it; the adapter test uses it).
- The 500 rule also applies to the web page and `/clear`, not just the API.
- Table name pinned to `callback` via `@MappedEntity("callback")`.
- Deps inline, no version catalog.
- Controllers log the error on a `Left` before returning 500 (as encore's `LoginEventListener` does), since the stack
  trace Micronaut used to log is now lost inside `PersistenceError`.
- `DbCallback.toDomain()` uses `requireNotNull` on `id`, following encore, instead of the old `id ?: 0`.
- The newest-first ordering test seeds through `DbCallbackRepository` (to control timestamps), then asserts through
  `ListCallbacksPort`.

## Left to the builder

- File splits for small types (errors, mapper), KDoc on ports (encore has brief KDoc; follow it).
- Exact test names and how adapter tests are split.
- Commit granularity.
