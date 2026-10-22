# Live inbox

## Intent

Idea 1 in `docs/ideas.md`. The open inbox page shows each callback as soon as it arrives, instead of polling
`GET /inbox` every 5 seconds while a checkbox is ticked. The UI gets better and the page gets simpler.

## Goals

- The page opens one server-sent events stream when it loads. Whenever the callbacks change, the server pushes the
  re-rendered `inbox` fragments (count, Clear All, list) down it, and Datastar swaps them in as it does today.
- The stream sends the current inbox as soon as it (re)connects, so nothing is missed while it was closed (Datastar
  closes it while the tab is hidden and reconnects after errors).
- Clear All and single deletes also wake the stream, so other open tabs update too.
- The auto-refresh checkbox, its `autoRefresh` signal and the `data-on-interval` div are deleted.

## Non-goals

- Removing the Refresh button. It stays, like the refresh in an email inbox.
- Pushing only the new card. Card ids are positional (`callback-1`…) and the count and Clear All change too.

## Anti-goals

- WireMock, Micronaut or Reactor types under `domain`.
- The web adapter talking to `WireMockServer` directly instead of through a port.
- A new dependency. Reactor already comes with `micronaut-http-server-netty`.

## Constraints

- `WireMockServer.addMockServiceRequestListener` fires **before** the request is written to the journal, so listing
  callbacks from it would miss the new one. A WireMock `ServeEventListener` extension's `afterComplete` runs after
  `requestJournal.serveCompleted`, so that is the hook. Admin (`/__admin`) requests don't go through it.
- `ArchitectureTest` enforces ports and adaptors, so the "callbacks changed" signal is a domain port.

## Approaches

- **A. Push the rendered inbox (chosen).** The server renders the `inbox` template to a string and sends it as a
  Datastar `datastar-patch-elements` event (`mode replace`, as the `/inbox` endpoints use today). It is what
  Datastar is built for, and the stream and the Refresh button share the same fragments. The cost is rendering
  Thymeleaf in code instead of through `@View`.
- **B. Push a nudge, the page fetches.** The stream sends a signal and the page calls `GET /inbox`. Less server code,
  but two round trips per update, with the real update hidden behind a signal watcher.
- **C. Push only the new card.** Rejected, see *Non-goals*.

## Decided

- A new framework-free domain port lets the web adapter subscribe to "callbacks changed". The WireMock adapter
  implements it with a `ServeEventListener` registered in `WireMockFactory`.
- The web controller fans changes out to every open stream (for example a Reactor sink), and also emits after a
  successful clear or delete.
- The stream lives on its own element in `callbacks.html` (`data-init="@get(...)"`), so the Refresh button's
  request doesn't cancel it.
- The empty-inbox hint "Send a request and press Refresh to see it here" changes to say it shows up on its own.

## Left to the builder

- The port's name and shape, the stream's path, and how the template is rendered to a string (`ViewsRenderer` or
  the Thymeleaf engine).
- Whether the `inbox-error` notification should react to stream failures differently from a failed Refresh.
- README: mention that the inbox updates live.
