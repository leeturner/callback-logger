# Copy as curl Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Each callback card gets a "Copy as curl" button, plus a "curl" panel showing a command that sends the same
request back to the callback logger.

**Architecture:** A pure Kotlin function in the web adapter, `Callback.toCurl(baseUrl)`, builds the command.
`toView` takes the base URL and stores the result in a new `CallbackView.curl` field. The `inbox` template renders it
in a `<details>` panel, and a small `copyCurl` function in `callbacks.html` copies it, opening the panel if the
clipboard is refused.

**Tech Stack:** Kotlin, Micronaut 5 (Thymeleaf views), Datastar, Bulma, JUnit 6, Strikt, RestAssured.

**Spec:** `docs/superpowers/specs/2026-10-22-copy-as-curl-design.md`

## Global Constraints

- Target: `http://localhost:<callback-logger.wiremock.port><uri>`, the uri as stored (path plus query string).
- Always `-X <method>`.
- Dropped headers, matched case-insensitively: `Host`, `Content-Length`, `Connection`, `Accept-Encoding`.
- One `-H` per header value.
- Non-empty body → `--data-raw`; empty body → no data flag.
- Every argument single-quoted; `'` inside becomes `'\''`.
- No new dependencies. No quoting logic in JavaScript.
- Nothing under `com.leeturner.callback_logger.domain` changes (`ArchitectureTest` must stay green).
- Commit with `git commit --no-gpg-sign`, message ending `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `./gradlew test` green at the end of every task (baseline: green).

## Review Focus

1. A body containing `'` (for example `{"name": "it's"}`) must still paste as one valid shell argument. (Task 1)
2. A header with an empty value: curl drops `-H 'X-Empty:'`, so it must be written as `-H 'X-Empty;'` to be sent.
   (Task 1)
3. Lower-case `host` / `content-length` (HTTP/2 or proxies) must be dropped too. (Task 1)
4. A body with newlines (pretty-printed JSON) stays inside one quoted argument; nothing is joined or trimmed. (Task 1)
5. The clipboard API is missing on plain HTTP to a non-localhost host: `navigator.clipboard` is `undefined`, so
   calling `writeText` throws synchronously instead of rejecting. The panel must still open. (Task 2, manual check)

---

### Task 1: Build the curl command

**Files:**
- Create: `src/main/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CurlCommand.kt`
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CurlCommandTest.kt`

**Interfaces:**
- Produces: `internal fun Callback.toCurl(baseUrl: String): String` in `CurlCommand.kt`. `baseUrl` has no trailing
  slash, for example `http://localhost:7071`.

Output format: the arguments joined with ` \` and a newline plus two spaces, in this order: `curl -X '<method>' '<url>'`
on the first line, then each `-H`, then `--data-raw`. Example:

```
curl -X 'POST' 'http://localhost:7071/hook?a=1' \
  -H 'Content-Type: application/json' \
  -H 'X-Signature: sha256=abc' \
  --data-raw '{"name": "it'\''s"}'
```

- [ ] **Step 1: Write the failing tests**

Plain JUnit, no `@MicronautTest`. A private `callback(...)` helper in the test builds a `Callback` with defaults
(method `POST`, uri `/hook`, empty headers, empty payload), so each test only sets what it checks.

- `a post with a body becomes curl with the method, url, headers and data`: method `POST`, uri `/hook?a=1`,
  headers `Content-Type: [application/json]`, `X-Signature: [sha256=abc]`, payload `{"hello": "world"}`.
  `toCurl("http://localhost:7071")` equals exactly
  `"curl -X 'POST' 'http://localhost:7071/hook?a=1' \\\n  -H 'Content-Type: application/json' \\\n  -H 'X-Signature: sha256=abc' \\\n  --data-raw '{\"hello\": \"world\"}'"`.
- `a callback without a body has no data flag`: method `GET`, uri `/ping` → equals
  `"curl -X 'GET' 'http://localhost:7071/ping'"`.
- `headers curl sets itself are left out whatever their case`: headers `Host`, `content-length`, `CONNECTION`,
  `Accept-Encoding`, `X-Keep` → result does not contain `-H 'Host`, `-H 'content-length`, `-H 'CONNECTION`,
  `-H 'Accept-Encoding` (strikt `not().contains(...)`) and contains `-H 'X-Keep: 1'`.
- `a header with several values becomes one -H per value`: `X-Multi: [a, b]` → contains `-H 'X-Multi: a'` and
  `-H 'X-Multi: b'`.
- `a header with an empty value is sent empty`: `X-Empty: [""]` → contains `-H 'X-Empty;'`.
- `single quotes are escaped for the shell`: payload `{"name": "it's"}`, uri `/it's` → contains
  `--data-raw '{"name": "it'\''s"}'` and `'http://localhost:7071/it'\''s'`.
- `a multi-line body stays in one argument`: payload `"{\n  \"a\": 1\n}"` → ends with
  `"--data-raw '{\n  \"a\": 1\n}'"`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests '*CurlCommandTest'`
Expected: compilation failure, `Unresolved reference 'toCurl'`.

- [ ] **Step 3: Implement `Callback.toCurl(baseUrl: String): String` in `CurlCommand.kt`**

A private `quote(value: String): String` (wraps in `'`, replaces `'` with `'\''`) and a private set of the dropped
header names, lower-cased. Empty header value → `Name;`, otherwise `Name: value`. Comment density as in
`CallbackView.kt`: one line explaining why those headers are dropped, one for the `;` trick.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests '*CurlCommandTest'`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CurlCommand.kt \
        src/test/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CurlCommandTest.kt
git commit --no-gpg-sign -m "Build a curl command for a callback

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Show and copy the command on each card

**Files:**
- Modify: `src/main/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CallbackView.kt` (add `curl`, `toView(baseUrl)`)
- Modify: `src/main/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CallbackWebController.kt` (`inboxModel`)
- Modify: `src/main/resources/views/inbox.html` (copy button, curl panel)
- Modify: `src/main/resources/views/callbacks.html` (`copyCurl` script, panel styles)
- Modify: `README.md` (one sentence in Usage)
- Test: `src/test/kotlin/com/leeturner/callback_logger/adapters/inbound/web/CallbackWebListViewMultipleCallbacksTest.kt`

**Interfaces:**
- Consumes: `Callback.toCurl(baseUrl: String): String` from Task 1.
- Produces: `CallbackView.curl: String` (constructor field, last), `fun Callback.toView(baseUrl: String): CallbackView`.

- [ ] **Step 1: Write the failing tests** in `CallbackWebListViewMultipleCallbacksTest`

Inject `@Property(name = "callback-logger.wiremock.port") private val wireMockPort: Int` into the constructor, as
`CallbackWebListViewZeroCallbacksTest` does.

- `each callback has a curl panel that sends it back to the callback logger`: `server.resetRequests()`,
  `sendCallback(server, "/hook?a=1", TEST_JSON_PAYLOAD, headers = mapOf("X-Signature" to "sha256=abc"))`, then on
  `inboxPage(spec)`, `getString("html.body.'**'.find {it.@id == 'callback-1-curl'}.pre.text()")` contains
  `curl -X 'POST' 'http://localhost:$wireMockPort/hook?a=1'`, `-H 'X-Signature: sha256=abc'` and
  `--data-raw '$TEST_JSON_PAYLOAD'`.
- `each callback has a copy as curl button`: `getString("html.body.'**'.find {it.@id == 'callback-1-copy-curl'}.@aria-label")`
  equals `Copy as curl`, and the raw markup contains `onclick="copyCurl(this)"`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests '*CallbackWebListViewMultipleCallbacksTest'`
Expected: the two new tests FAIL (element not found / null), the rest pass.

- [ ] **Step 3: Wire the command into the view**

`toView(baseUrl)` sets `curl = this.toCurl(baseUrl)`. In `inboxModel`, introduce
`val callbackBaseUrl = "http://localhost:$wireMockPort"` and use it for both `toView(callbackBaseUrl)` and the
existing `callbackUrl` (`"$callbackBaseUrl/callback"`).

- [ ] **Step 4: Add the button and panel to `inbox.html`**

- Copy button in the card header, just before the delete button: id `callback-N-copy-curl`, classes
  `button is-small is-link is-outlined ml-3`, `title` and `aria-label` `Copy as curl`, `onclick="copyCurl(this)"`
  (plain `onclick`, like the theme toggle; the copy needs no server call). Icon: an inline clipboard SVG in the same
  style as the delete icon (`width/height 16`, `stroke="currentColor"`, `aria-hidden="true"`).
- Panel after Query parameters in `.details`: `<details>` with id `callback-N-curl`, class `curl`,
  `th:data-panel="${callback.id + '-curl'}"`, `data-init="keepOpen(el)"`, `<summary>curl</summary>`, then
  `<pre class="is-size-7 mt-2" th:text="${callback.curl}">`.

- [ ] **Step 5: Add `copyCurl` and styles to `callbacks.html`**

`function copyCurl(button)` next to `keepOpen`. It finds the card's `details.curl` and copies its `pre`'s
`textContent` with `Promise.resolve().then(() => navigator.clipboard.writeText(text))`, so the synchronous throw when
`navigator.clipboard` is `undefined` and a rejected write land in the same `.catch`. On success it adds `is-success`
to the button for 1.5 s; the `.catch` sets the panel's `open = true`, which `keepOpen` then remembers. One comment line
saying why the panel opens. A stream patch arriving mid-flash replaces the button and drops the highlight; that's
fine, leave it.

Style: `.callback .details pre { white-space: pre-wrap; word-break: break-all; }` so long URLs and bodies wrap.

- [ ] **Step 6: README**

In Usage, after the paragraph on sending a callback with `curl`: one sentence saying each callback in the inbox has a
"Copy as curl" button that copies a command to send it to the callback logger again.

In `docs/ideas.md`, change idea 3's heading to `### 3. Copy as curl / replay (copy as curl done)`.

- [ ] **Step 7: Run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Manual check in the browser**

`./gradlew run`, send a callback (README example), open `http://localhost:7070/callback-logger/`. Click "Copy as
curl", paste it into a terminal and run it: a second identical callback appears in the inbox. Then open the page as
`http://<your LAN IP>:7070/callback-logger/` (not a secure context) and click it again: the curl panel opens.

- [ ] **Step 9: Commit**

```bash
git add README.md docs/ideas.md src/main src/test
git commit --no-gpg-sign -m "Copy a callback as curl from the inbox

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
