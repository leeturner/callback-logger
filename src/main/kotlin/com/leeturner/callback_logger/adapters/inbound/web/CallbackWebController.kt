package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.DeleteCallbackPort
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.views.View
import org.slf4j.LoggerFactory

@Controller
class CallbackWebController(
    private val listCallbacksPort: ListCallbacksPort,
    private val clearCallbacksPort: ClearCallbacksPort,
    private val deleteCallbackPort: DeleteCallbackPort,
    @Property(name = "callback-logger.wiremock.port")
    private val wireMockPort: Int,
) {

  @View("callbacks")
  @Get("/")
  fun index(): HttpResponse<Map<String, Any>> = HttpResponse.ok(inboxModel())

  // Renders just the parts of the page that change. Datastar patches them into the open page by their ids, so the
  // inbox can be refreshed without a full page reload.
  @View("inbox")
  @Get("/inbox")
  fun inbox(): HttpResponse<Map<String, Any>> = inboxResponse()

  @View("inbox")
  @Delete("/inbox")
  fun clear(): HttpResponse<Map<String, Any>> =
      clearCallbacksPort.clear().fold(
          { error ->
            logger.error("Failed to clear callbacks: {}", error)
            // thrown rather than returned, as @View would render a 500 response as an empty inbox
            throw IllegalStateException("Failed to clear callbacks: $error")
          },
          { inboxResponse() },
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
          { inboxResponse() },
      )

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
            mapOf(
                "callbacks" to callbacks.map { it.toView() },
                "callbackCount" to callbacks.size,
                // WireMock receives callbacks on its own port, on any path; this is just an example for the empty inbox
                "callbackUrl" to "http://localhost:$wireMockPort/callback",
            )
          },
      )

  companion object {
    private val logger = LoggerFactory.getLogger(CallbackWebController::class.java)
    const val DATASTAR_MODE_HEADER = "datastar-mode"
  }
}
