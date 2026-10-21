package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.uri
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View
import org.slf4j.LoggerFactory

@Controller
class CallbackWebController(
    @Property(name="micronaut.server.context-path") private val contextPath: String,
    private val listCallbacksPort: ListCallbacksPort,
    private val clearCallbacksPort: ClearCallbacksPort,
) {

  @View("callbacks")
  @Get("/")
  fun index(): HttpResponse<Map<String, Any>> =
      listCallbacksPort.listNewestFirst().fold(
          { error ->
            logger.error("Failed to list callbacks: {}", error)
            // thrown rather than returned, as @View would render a 500 response as an empty inbox
            throw IllegalStateException("Failed to list callbacks: $error")
          },
          { callbacks ->
            HttpResponse.ok(mapOf("callbacks" to callbacks.map { it.toView() }, "callbackCount" to callbacks.size))
          },
      )

  @Get("/clear")
  fun clear(): HttpResponse<String> {
    // delete all the callbacks from the database and redirect to the homepage.  This means that we won't be
    // left with the /clear path on the url in the browser which could be problematic if the user tries to 
    // refresh to see new callbacks and inadvertently deletes them all.
    return clearCallbacksPort.clear().fold(
        { error ->
          logger.error("Failed to clear callbacks: {}", error)
          HttpResponse.serverError()
        },
        { HttpResponse.temporaryRedirect(uri("/$contextPath/")) },
    )
  }

  companion object {
    private val logger = LoggerFactory.getLogger(CallbackWebController::class.java)
  }
}
