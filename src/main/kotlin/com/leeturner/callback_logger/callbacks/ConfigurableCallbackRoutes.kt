package com.leeturner.callback_logger.callbacks

import io.micronaut.context.ExecutionHandleLocator
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.web.router.DefaultRouteBuilder
import io.micronaut.web.router.RouteBuilder
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ConfigurableCallbackRoutes(
    executionHandleLocator: ExecutionHandleLocator,
    uriNamingStrategy: RouteBuilder.UriNamingStrategy,
) : DefaultRouteBuilder(executionHandleLocator, uriNamingStrategy) {

  @Inject
  fun configurableCallbackRoutes(
      callbackApiController: CallbackApiController,
      callbackLoggerConfiguration: CustomCallbackConfiguration
  ) {
//    println(callbackLoggerConfiguration.myMessage)
//    println(callbackLoggerConfiguration.customCallbacks)
    println(callbackLoggerConfiguration)
    PUT(
            "/api/put",
            callbackApiController,
            "putCallback",
            String::class.java,
            HttpRequest::class.java)
        .consumes(MediaType.ALL_TYPE)

    println(uriRoutes)
  }
}
