package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.TEST_XML_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@MicronautTest
class CallbackReceiptTest(private val server: WireMockServer) {

  @BeforeEach
  fun setUp() {
    server.resetRequests()
  }

  @ParameterizedTest
  @ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "PROPFIND"])
  internal fun `a callback on any method and nested path gets the configured response`(method: String) {
    sendCallback(server, "/any/nested/path?a=1", TEST_JSON_PAYLOAD, method = method)
        .statusCode(200)
        .contentType("application/json")
        .body(equalTo(""))

    expectThat(server.allServeEvents.single().request) {
      get { this.method.name } isEqualTo method
      get { url } isEqualTo "/any/nested/path?a=1"
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value =
          [
              "${MediaType.APPLICATION_JSON};${TEST_JSON_PAYLOAD}",
              "${MediaType.APPLICATION_XML};${TEST_XML_PAYLOAD}"])
  internal fun `a callback body is recorded whatever its media type`(mediaType: String, payload: String) {
    sendCallback(server, "/callback", payload, mediaType).statusCode(200)

    expectThat(server.allServeEvents.single().request.bodyAsString) isEqualTo payload
  }

  @Test
  internal fun `the admin api is refused and nothing is recorded`() {
    sendCallback(server, "/__admin/mappings", method = "GET").statusCode(401)

    expectThat(server.allServeEvents).isEmpty()
  }

  @Test
  internal fun `shutdown through the admin api is refused and callbacks keep arriving`() {
    sendCallback(server, "/__admin/shutdown").statusCode(401)

    sendCallback(server, "/callback", TEST_JSON_PAYLOAD).statusCode(200)
  }
}
