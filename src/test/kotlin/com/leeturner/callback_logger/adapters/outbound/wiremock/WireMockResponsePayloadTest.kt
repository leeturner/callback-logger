package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@MicronautTest(environments = ["payload"])
class WireMockResponsePayloadTest(private val server: WireMockServer) {

  @ParameterizedTest
  @ValueSource(strings = ["POST", "PUT"])
  internal fun `a callback returns the custom configured payload`(method: String) {
    sendCallback(server, "/callback", TEST_JSON_PAYLOAD, method = method)
        .statusCode(200)
        .contentType("application/json")
        .body(equalTo("""{"status": "OK", "message": "Callback received"}"""))
  }
}
