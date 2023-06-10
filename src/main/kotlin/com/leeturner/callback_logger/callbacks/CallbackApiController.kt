package com.leeturner.callback_logger.callbacks

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@ExecuteOn(TaskExecutors.IO)
@Controller("/api")
class CallbackApiController(
    private val callbackService: CallbackService,
    @Property(name = "callback-logger.callback-response-status-code")
    private val callbackResponseStatusCode: HttpStatus,
) {

  @Post("/callback")
  @Consumes(MediaType.ALL)
  fun postCallback(@Body body: String, request: HttpRequest<Any>): HttpResponse<String> {
    saveAndPrintCallback(body, request)
    return HttpResponse.status(callbackResponseStatusCode)
  }

  @Put("/callback")
  @Consumes(MediaType.ALL)
  fun putCallback(@Body body: String, request: HttpRequest<Any>): HttpResponse<String> {
    saveAndPrintCallback(body, request)
    return HttpResponse.status(callbackResponseStatusCode)
  }

  private fun saveAndPrintCallback(body: String, request: HttpRequest<Any>) {
    println()
    println(body)
    println()
    println(request.method)
    println(request.httpVersion)
    println(request.uri)
    println()
    request.headers.forEach { println("${it.key} -> ${it.value}") }

    callbackService.saveCallback(
        request.uri.toString(), request.method.name, request.httpVersion.name, body)
  }
}
