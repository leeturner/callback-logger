package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType

/**
 * WireMock receives every callback on its own port and keeps them in its request journal, which is the callback store.
 */
@Factory
class WireMockFactory {

  // @Context starts WireMock with the app, so callbacks are accepted before anyone opens the inbox
  @Context
  @Bean(preDestroy = "stop")
  fun wireMockServer(
      @Property(name = "callback-logger.wiremock.port") port: Int,
      @Property(name = "callback-logger.callback-response-status-code") status: HttpStatus,
      @Property(name = "callback-logger.callback-response-payload") payload: String,
      @Property(name = "callback-logger.callback-response-content-type") contentType: String,
      watcher: WireMockCallbackWatcherAdapter,
  ): WireMockServer {
    // checked before WireMock starts, so a bad value stops the app without leaving the port bound
    val responseContentType = validContentType(contentType.ifBlank { MediaType.APPLICATION_JSON })
    return WireMockServer(
            wireMockConfig()
                .port(port)
                .mappingSource(NoMappingsSource)
                .notifier(ConsoleNotifier(true))
                .extensions(watcher)
                // the app reads the journal through the WireMockServer object, so the admin API stays locked
                .adminAuthenticator { false })
        .apply {
          start()
          stubFor(
              any(anyUrl())
                  .willReturn(
                      aResponse()
                          .withStatus(status.code)
                          .withHeader("Content-Type", responseContentType)
                          .withBody(payload)))
        }
  }

  // MediaType.of only rejects values with no slash, so the type and subtype are checked as HTTP tokens too
  private fun validContentType(value: String): String {
    val mediaType = MediaType.of(value)
    require(TOKEN.matches(mediaType.type) && TOKEN.matches(mediaType.subtype)) {
      "Invalid callback-response-content-type: $value"
    }
    return value
  }

  private companion object {
    val TOKEN = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
  }
}
