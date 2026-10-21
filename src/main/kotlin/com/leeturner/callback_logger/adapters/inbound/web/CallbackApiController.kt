package com.leeturner.callback_logger.adapters.inbound.web

import arrow.core.Either
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.NewCallback
import com.leeturner.callback_logger.domain.callbacks.SaveCallbackError
import com.leeturner.callback_logger.domain.callbacks.ports.SaveCallbackPort
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
import org.slf4j.LoggerFactory

@ExecuteOn(TaskExecutors.IO)
@Controller("/api")
class CallbackApiController(
    private val saveCallbackPort: SaveCallbackPort,
    @Property(name = "callback-logger.callback-response-status-code")
    private val callbackResponseStatusCode: HttpStatus,
    @Property(name = "callback-logger.callback-response-payload")
    private val callbackResponsePayload: String,
) {

  @Post("/callback")
  @Consumes(MediaType.ALL)
  fun postCallback(@Body body: String, request: HttpRequest<Any>): HttpResponse<String> =
      respond(saveAndPrintCallback(body, request))

  @Put("/callback")
  @Consumes(MediaType.ALL)
  fun putCallback(@Body body: String, request: HttpRequest<Any>): HttpResponse<String> =
      respond(saveAndPrintCallback(body, request))

  private fun respond(saved: Either<SaveCallbackError, Callback>): HttpResponse<String> =
      saved.fold(
          { error ->
            logger.error("Failed to save callback: {}", error)
            HttpResponse.serverError()
          },
          { HttpResponse.status<String>(callbackResponseStatusCode).body(callbackResponsePayload) },
      )

  private fun saveAndPrintCallback(body: String, request: HttpRequest<Any>): Either<SaveCallbackError, Callback> {
    println()
    println(body)
    println()
    println(request.method)
    println(request.httpVersion)
    println(request.uri)
    println()
    request.headers.forEach { println("${it.key} -> ${it.value}") }

    return saveCallbackPort.save(
        NewCallback(
            uri = request.uri.toString(),
            httpMethod = request.method.name,
            httpVersion = request.httpVersion.name,
            payload = body,
        ))
  }

  companion object {
    private val logger = LoggerFactory.getLogger(CallbackApiController::class.java)
  }
}
