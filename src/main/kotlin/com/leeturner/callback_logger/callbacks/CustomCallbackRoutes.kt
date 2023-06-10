package com.leeturner.callback_logger.callbacks

import io.micronaut.context.ExecutionHandleLocator
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.web.router.DefaultRouteBuilder
import io.micronaut.web.router.RouteBuilder
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CustomCallbackRoutes(
    executionHandleLocator: ExecutionHandleLocator,
    uriNamingStrategy: RouteBuilder.UriNamingStrategy,
) : DefaultRouteBuilder(executionHandleLocator, uriNamingStrategy) {

  @Inject
  fun configurableCallbackRoutes(
      callbackApiController: CallbackApiController,
      customCallbackConfiguration: CustomCallbackConfiguration,
  ) {
    // first lets loop through the post callbacks removing duplicates first
    customCallbackConfiguration.post.distinct().forEach {
      // validate post urls here
      println("Registering custom POST callback URL: $it")
      POST(it, callbackApiController, "postCallback", String::class.java, HttpRequest::class.java)
          .consumes(MediaType.ALL_TYPE)
    }
    // now lets loop through the put callbacks removing duplicates first
    customCallbackConfiguration.put.distinct().forEach {
      // validate put urls here
      println("Registering custom PUT callback URL: $it")
      PUT(it, callbackApiController, "putCallback", String::class.java, HttpRequest::class.java)
          .consumes(MediaType.ALL_TYPE)
    }
  }
}
