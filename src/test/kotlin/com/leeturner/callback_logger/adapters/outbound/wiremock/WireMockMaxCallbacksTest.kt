package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest(environments = ["max-callbacks"])
class WireMockMaxCallbacksTest(private val server: WireMockServer) {

  @Test
  internal fun `only the newest callbacks are kept once the configured maximum is reached`() {
    listOf("/first", "/second", "/third").forEach { sendCallback(server, it).statusCode(200) }

    assertEquals(listOf("/third", "/second"), server.allServeEvents.map { it.request.url })
  }
}
