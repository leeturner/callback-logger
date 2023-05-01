package com.leeturner.callback_logger.callbacks

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View

@Controller("/")
class CallbackWebController(
    private val callbackService: CallbackService,
) { 
    
    @View("callbacks")
    @Get
    fun index() : HttpResponse<Map<String, Any>>  {
        val callbacks = callbackService.getCallbacksOrderedByTimestampDesc()
        return HttpResponse.ok(mapOf("callbacks" to callbacks, "callbackCount" to callbacks.size))
    }
}