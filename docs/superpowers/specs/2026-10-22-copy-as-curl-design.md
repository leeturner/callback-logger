# Copy as curl

## Intent

Idea 3 in `docs/ideas.md`, first half. Each callback card gets a "Copy as curl" button. It puts a `curl` command on the
clipboard that sends the same request back to the callback logger, so a copy of that callback arrives in the inbox.

## Goals

- The command targets `http://localhost:<wiremock port><uri>`, the same base URL as the empty-inbox example. The
  query string comes with the URI.
- It always has `-X <method>`.
- It keeps every header except `Host`, `Content-Length`, `Connection` and `Accept-Encoding` (matched
  case-insensitively). curl sets those itself, and copying them can break the request. A header with several values
  becomes one `-H` per value.
- A non-empty body goes in as `--data-raw`. With no body, there's no data flag.
- Every argument is single-quoted for the shell, with `'` written as `'\''`, so JSON with apostrophes still works.
- If the browser refuses the clipboard (plain HTTP on a host other than localhost), the command is still visible in a
  panel on the card and can be selected and copied by hand.

## Non-goals

- Replaying to a URL you choose (the second half of idea 3).
- Binary bodies round-tripping exactly. The payload is already a string.
- Windows `cmd` or PowerShell quoting.

## Anti-goals

- Quoting logic in untested JavaScript.
- A new dependency.

## Approaches

- **A. Build the command in Kotlin (chosen).** A `curl` property, or a function taking the base URL, on
  `CallbackView` next to `formattedBodySize`. The template renders it, and a one-line `navigator.clipboard.writeText`
  copies it. The filtering and quoting are plain Kotlin and covered by tests.
- **B. Build it in the browser.** No server change, but the quoting would be untested JS in `callbacks.html`.

## Decided

- The command is built server-side (A).
- A copy button sits in the card header next to the delete button.
- A collapsible "curl" panel sits alongside Headers and Query parameters, showing the command in a `<pre>`, and
  remembers whether it's open like the others do (`keepOpen`). If the clipboard write fails, the panel opens.

## Left to the builder

- How the WireMock base URL reaches the view (passed into `toView`, or as an argument from the template).
- Whether the command is on one line or split with ` \` continuations. Split reads better in the panel.
- Feedback on a successful copy (for example the button briefly showing a tick).
- README: mention the button.
