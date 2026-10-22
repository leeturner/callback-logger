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
  ): WireMockServer =
      WireMockServer(
              wireMockConfig()
                  .port(port)
                  .notifier(ConsoleNotifier(true))
                  // the app reads the journal through the WireMockServer object, so the admin API stays locked
                  .adminAuthenticator { false })
          .apply {
            start()
            stubFor(
                any(anyUrl())
                    .willReturn(
                        aResponse()
                            .withStatus(status.code)
                            .withHeader("Content-Type", "application/json")
                            .withBody(payload)))
          }
}
