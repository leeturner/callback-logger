package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.DeleteCallbackPort
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.WatchCallbacksPort
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.sse.Event
import io.micronaut.views.View
import io.micronaut.views.ViewsRenderer
import java.io.StringWriter
import java.time.Duration
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers

@Controller
class CallbackWebController(
    private val listCallbacksPort: ListCallbacksPort,
    private val clearCallbacksPort: ClearCallbacksPort,
    private val deleteCallbackPort: DeleteCallbackPort,
    watchCallbacksPort: WatchCallbacksPort,
    private val viewsRenderer: ViewsRenderer<Map<String, Any>, HttpRequest<*>>,
    @Property(name = "callback-logger.wiremock.port")
    private val wireMockPort: Int,
) {
  // directBestEffort, as the other multicast sinks shut down for good when the last open page goes away
  private val changes = Sinks.many().multicast().directBestEffort<Unit>()

  init {
    watchCallbacksPort.onChange(::changed)
  }

  @View("callbacks")
  @Get("/")
  fun index(): HttpResponse<Map<String, Any>> = HttpResponse.ok(inboxModel())

  // Renders just the parts of the page that change. Datastar patches them into the open page by their ids, so the
  // inbox can be refreshed without a full page reload.
  @View("inbox")
  @Get("/inbox")
  fun inbox(): HttpResponse<Map<String, Any>> = inboxResponse()

  // Each open page keeps this stream open, and gets the inbox fragments again whenever the callbacks change
  @Get("/inbox/stream", produces = [MediaType.TEXT_EVENT_STREAM])
  fun stream(request: HttpRequest<*>): Publisher<Event<String>> =
      // starts with Unit so a (re)connecting page gets the current inbox straight away
      Flux.concat(Flux.just(Unit), changes.asFlux())
          // a burst of callbacks becomes one render of the latest inbox, off WireMock's thread
          .onBackpressureLatest()
          .publishOn(Schedulers.boundedElastic(), 1)
          .map { Event.of(patchElements(render(request))).name("datastar-patch-elements") }
          // Netty closes a connection after 5 minutes with no reads or writes
          .mergeWith(Flux.interval(HEARTBEAT).map { Event.of("").comment("keep-alive") })

  @View("inbox")
  @Delete("/inbox")
  fun clear(): HttpResponse<Map<String, Any>> =
      clearCallbacksPort.clear().fold(
          { error ->
            logger.error("Failed to clear callbacks: {}", error)
            // thrown rather than returned, as @View would render a 500 response as an empty inbox
            throw IllegalStateException("Failed to clear callbacks: $error")
          },
          {
            changed()
            inboxResponse()
          },
      )

  @View("inbox")
  @Delete("/inbox/{id}")
  fun delete(id: String): HttpResponse<Map<String, Any>> =
      deleteCallbackPort.delete(id).fold(
          { error ->
            logger.error("Failed to delete callback {}: {}", id, error)
            // thrown rather than returned, as @View would render a 500 response as an empty inbox
            throw IllegalStateException("Failed to delete callback $id: $error")
          },
          {
            changed()
            inboxResponse()
          },
      )

  // emits come from WireMock's and Netty's threads, and the sink refuses ones that overlap
  @Synchronized
  private fun changed() {
    changes.tryEmitNext(Unit)
  }

  private fun render(request: HttpRequest<*>): String =
      StringWriter().also { viewsRenderer.render("inbox", inboxModel(), request).writeTo(it) }.toString()

  // Datastar wants every line of the html as its own `elements` data line; Micronaut writes each line as `data: `
  private fun patchElements(html: String): String =
      "mode replace\n" + html.lines().joinToString("\n") { "elements $it" }

  // `replace` swaps the fragments in rather than morphing them, so each payload is highlighted again by its data-init
  private fun inboxResponse(): HttpResponse<Map<String, Any>> =
      HttpResponse.ok(inboxModel()).header(DATASTAR_MODE_HEADER, "replace")

  private fun inboxModel(): Map<String, Any> =
      listCallbacksPort.listNewestFirst().fold(
          { error ->
            logger.error("Failed to list callbacks: {}", error)
            // thrown rather than returned, as @View would render a 500 response as an empty inbox
            throw IllegalStateException("Failed to list callbacks: $error")
          },
          { callbacks ->
            // WireMock receives callbacks on its own port, on any path
            val callbackBaseUrl = "http://localhost:$wireMockPort"
            mapOf(
                "callbacks" to callbacks.map { it.toView(callbackBaseUrl) },
                "callbackCount" to callbacks.size,
                // just an example for the empty inbox
                "callbackUrl" to "$callbackBaseUrl/callback",
            )
          },
      )

  companion object {
    private val logger = LoggerFactory.getLogger(CallbackWebController::class.java)
    const val DATASTAR_MODE_HEADER = "datastar-mode"
    private val HEARTBEAT = Duration.ofSeconds(30)
  }
}
