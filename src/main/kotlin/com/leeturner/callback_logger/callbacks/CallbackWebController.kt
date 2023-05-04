package com.leeturner.callback_logger.callbacks

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.uri
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View

@Controller
class CallbackWebController(
    @Property(name="micronaut.server.context-path") private val contextPath: String,
    private val callbackService: CallbackService,
) {

  @View("callbacks")
  @Get("/")
  fun index(): HttpResponse<Map<String, Any>> {
    val callbacks = callbackService.getCallbacksOrderedByTimestampDesc()
    return HttpResponse.ok(mapOf("callbacks" to callbacks, "callbackCount" to callbacks.size))
  }

  @Get("/clear")
  fun clear(): HttpResponse<String> {
    // delete all the callbacks from the database and redirect to the homepage.  This means that we won't be
    // left with the /clear path on the url in the browser which could be problematic if the user tries to 
    // refresh to see new callbacks and inadvertently deletes them all.
    callbackService.clearAllCallbacks()
    return HttpResponse.temporaryRedirect(uri("/$contextPath/"))
  }
}
