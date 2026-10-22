package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@MicronautTest(environments = ["status"])
class WireMockResponseStatusTest(private val server: WireMockServer) {

  @ParameterizedTest
  @ValueSource(strings = ["POST", "PUT"])
  internal fun `a callback returns the custom configured status code`(method: String) {
    sendCallback(server, "/callback", TEST_JSON_PAYLOAD, method = method).statusCode(201)
  }
}
