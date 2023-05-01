package com.leeturner.callback_logger.callbacks

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@ExecuteOn(TaskExecutors.IO)
@Controller("/api")
class CallbackApiController(
  private val callbackService: CallbackService,
) {

  @Post("/callback")
  @Status(HttpStatus.OK)
  @Consumes(MediaType.ALL)
  fun callback(@Body body: String, request: HttpRequest<Any>) {
    println()
    println(body)
    println()
    println(request.method)
    println(request.httpVersion)
    request.headers.forEach { println("${it.key} -> ${it.value}") }

    callbackService.saveCallback(request.method.name, request.httpVersion.name, body)
  }
}
