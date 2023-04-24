package com.leeturner.callback_logger.api

import com.leeturner.callback_logger.api.model.CallbackDto
import com.leeturner.callback_logger.domain.Callback
import com.leeturner.callback_logger.repository.CallbackRepository
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@ExecuteOn(TaskExecutors.IO)
@Controller("/api")
class CallbackController(
    private val callbackRepository: CallbackRepository,
) {

  @Post("/callback")
  @Status(HttpStatus.OK)
  fun callback(@Body body: String, request: HttpRequest<Any>) {
    println()
    println(body)
    println()
    println(request.method)
    println(request.httpVersion)
    request.headers.forEach { println("${it.key} -> ${it.value}") }

    val callback =
        Callback(
            httpMethod = request.method.name,
            httpVersion = request.httpVersion.name,
            payload = body)
    callbackRepository.save(callback)
  }

  @Get("/callback")
  @Status(HttpStatus.OK)
  fun getCallbacks(): List<CallbackDto> {
    return callbackRepository.findAll().map { callback -> callback.toDto() }
  }

  private fun Callback.toDto() =
      CallbackDto(
          this.id ?: 0, this.timestamp.toString(), this.httpMethod, this.httpVersion, this.payload)
}
