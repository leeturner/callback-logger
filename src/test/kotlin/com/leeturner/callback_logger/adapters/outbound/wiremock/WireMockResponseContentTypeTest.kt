package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.TEST_XML_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThrows

@MicronautTest(environments = ["content-type"])
class WireMockResponseContentTypeTest(private val server: WireMockServer) {

  @Test
  internal fun `a callback returns the custom configured content type`() {
    sendCallback(server, "/callback", TEST_XML_PAYLOAD, contentType = "application/xml")
        .statusCode(200)
        .contentType("application/xml")
        .body(equalTo("<status>OK</status>"))
  }
}

class InvalidResponseContentTypeTest {

  @ParameterizedTest
  @ValueSource(strings = ["xml", "not a media type", "application/", "/xml", "application/xml/extra"])
  internal fun `the app fails to start when the response content type is not a valid media type`(
      contentType: String
  ) {
    expectThrows<Exception> {
      ApplicationContext.run(
              mapOf("callback-logger.callback-response-content-type" to contentType), "test")
          .close()
    }
  }
}
