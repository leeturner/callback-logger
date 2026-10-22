# Ideas

Possible next steps for callback-logger. Most of them build on WireMock now receiving callbacks and keeping them in
its request journal.

Suggested order: 1, then 4, then 2. Both 1 and 4 are done.

## Best fits for the current design

### 1. Live inbox instead of polling (done)

Datastar is built around server-sent events, and WireMock can report each request as it arrives through
`addMockServiceRequestListener`. Combining the two pushes new callbacks into the open page straight away. The 5-second
auto-refresh and its checkbox could then go, so the UI improves and the code probably gets smaller.

### 2. Slow or failing responses

WireMock already supports delays (`withFixedDelay`, random delays) and faults (`CONNECTION_RESET_BY_PEER`,
`EMPTY_RESPONSE`). One or two settings next to the existing `callback-response-*` ones would let you test how the
sender's retry and timeout logic copes. That's the next question people have after "did the callback arrive?"

### 3. Copy as curl / replay (copy as curl done)

A "copy as curl" button on each callback can be built in the template from data the inbox already shows. Replaying a
callback to a URL you choose is a bigger job but very useful when debugging a webhook handler.

## Smaller but worthwhile

### 4. Cap the request journal (done)

WireMock keeps every request in memory forever. Setting `maxRequestJournalEntries` with a sensible default (and making
it configurable) is a one-line safety net for an instance left running.

### 5. Filter the inbox

Callbacks can arrive on any path with any method, so the inbox gets noisy. Filtering by path or method would help.

### 6. JSON API for automated tests

For example, `GET /api/callbacks?path=/orders/42`, so a CI test can assert that a callback arrived instead of someone
checking the UI.

## Distribution

### 7. Publish a Docker image

Publish to GHCR from the existing build workflow. Needing Java 25 is a big hurdle for a tool people want to run
quickly. A past commit message mentions a Dockerfile, but there isn't one in the repo, so that's worth checking either
way.

## Not worth doing

### Webhook signature checks

Checking HMAC signatures (Stripe, GitHub and so on) means a different implementation for each provider. That's a lot of
work for a niche need.
