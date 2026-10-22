package com.leeturner.callback_logger

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.adapters.outbound.db.DbCallback
import io.micronaut.http.MediaType
import io.restassured.RestAssured.given
import io.restassured.response.ValidatableResponse

object TestFixtures {
  // Sends a callback to WireMock, which listens on its own port rather than Micronaut's
  fun sendCallback(
      server: WireMockServer,
      path: String,
      body: String = "",
      contentType: String = MediaType.APPLICATION_JSON,
      method: String = "POST",
  ): ValidatableResponse =
      given()
          .baseUri("http://localhost")
          .port(server.port())
          .contentType(contentType)
          .body(body)
          .request(method, path)
          .then()

  const val TEST_JSON_PAYLOAD = """{"hello": "world", "foo": "bar"}"""
  const val TEST_XML_PAYLOAD = """<a><test>XML Payload</test></a>"""

  val TEST_JSON_CALLBACK =
      DbCallback(
          uri = "/api/callback",
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TEST_JSON_PAYLOAD,
      )
  val TEST_XML_CALLBACK =
      DbCallback(
          uri = "/api/custom-callback",
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TEST_XML_PAYLOAD,
      )
}
