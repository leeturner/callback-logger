package com.leeturner.callback_logger.adapters.inbound.web

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.TEST_XML_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.path.xml.XmlPath
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@MicronautTest
class CallbackWebListViewMultipleCallbacksTest(private val server: WireMockServer) {

  // sent in this order, so the XML callback is the newest
  @BeforeEach
  fun setup() {
    server.resetRequests()
    sendCallback(server, "/api/callback", TEST_JSON_PAYLOAD)
    sendCallback(server, "/api/custom-callback", TEST_XML_PAYLOAD, MediaType.APPLICATION_XML)
  }

  @Test
  internal fun `the callback list view displays the correct callback count when there are callbacks saved`(
      spec: RequestSpecification
  ) {
    val response =
        spec
            .`when`()
            .get(CALLBACK_LOGGER_WEB_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract()
            .body()

    val linkPath = "html.body.'**'.find {it.@id == 'callback-count'}"
    expectThat(response.htmlPath()) { get { getInt("$linkPath.text()") } isEqualTo 2 }
  }
  
  @Test
  internal fun `the callback list has an enabled clear all button when there are callbacks`(
    spec: RequestSpecification
  ) {
    val response =
      spec
        .`when`()
        .get(CALLBACK_LOGGER_WEB_URL)
        .then()
        .assertThat()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .extract()
        .body()

    val buttonPath = "html.body.'**'.find {it.@id == 'clear-all-button'}"
    expectThat(response.htmlPath()) {
      get { getString("$buttonPath.text()").trim() } isEqualTo "Clear All"
      get { getString("$buttonPath.@disabled") }.isNull()
    }
  }
  
  @Test
  internal fun `clearing the inbox removes all callbacks and returns the empty inbox fragments`(
    spec: RequestSpecification
  ) {
    val response =
      spec
        .`when`()
        .delete("${CALLBACK_LOGGER_WEB_URL}inbox")
        .then()
        .assertThat()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .header("datastar-mode", "replace")
        .extract()
        .body()

    expectThat(response.htmlPath()) {
      get { getInt("html.body.'**'.find {it.@id == 'callback-count'}.text()") } isEqualTo 0
      get { getString("html.body.'**'.find {it.@id == 'clear-all-button'}.@disabled") } isEqualTo "disabled"
      get { getString("html.body.'**'.find {it.@id == 'empty-inbox'}.p[0].text()").trim() } isEqualTo
          "No callbacks yet"
    }
    expectThat(server.allServeEvents).isEmpty()
  }

  @Test
  internal fun `refreshing the inbox returns just the fragments that change`(
    spec: RequestSpecification
  ) {
    val response =
      spec
        .`when`()
        .get("${CALLBACK_LOGGER_WEB_URL}inbox")
        .then()
        .assertThat()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .header("datastar-mode", "replace")
        .extract()
        .body()

    expectThat(response.htmlPath()) {
      get { getInt("html.body.'**'.find {it.@id == 'callback-count'}.text()") } isEqualTo 2
      get { getString("html.body.'**'.find {it.@id == 'callback-1-payload'}.text()") } isEqualTo
          TEST_XML_PAYLOAD
      get { getString("html.body.'**'.find {it.@id == 'callback-2-payload'}.text()") } isEqualTo
          TEST_JSON_PAYLOAD
    }
    expectThat(response.asString()) { not().contains("<title>") }
  }

  @Test
  internal fun `the callback list view displays the callbacks in the correct order`(
      spec: RequestSpecification
  ) {
    val response =
        spec
            .`when`()
            .get(CALLBACK_LOGGER_WEB_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract()
            .body()

    // XML callback should be at the top of the list
    expectThat(response.htmlPath()) {
      get { getString("html.body.'**'.find {it.@id == 'callback-1-httpMethod'}.text()") } isEqualTo
          "POST"
      get { getString("html.body.'**'.find {it.@id == 'callback-1-httpVersion'}.text()") } isEqualTo
          "HTTP/1.1"
      get { getString("html.body.'**'.find {it.@id == 'callback-1-payload'}.text()") } isEqualTo
          TEST_XML_PAYLOAD
    }

    // Json callback should be next in the list
    expectThat(response.htmlPath()) {
      get { getString("html.body.'**'.find {it.@id == 'callback-2-httpMethod'}.text()") } isEqualTo
          "POST"
      get { getString("html.body.'**'.find {it.@id == 'callback-2-httpVersion'}.text()") } isEqualTo
          "HTTP/1.1"
      get { getString("html.body.'**'.find {it.@id == 'callback-2-payload'}.text()") } isEqualTo
          TEST_JSON_PAYLOAD
    }
  }

  @Test
  internal fun `a callback without a body shows No body`(spec: RequestSpecification) {
    server.resetRequests()
    sendCallback(server, "/ping", method = "GET")

    expectThat(inboxPage(spec)) {
      get { getString("html.body.'**'.find {it.@id == 'callback-1-payload'}.text()").trim() } isEqualTo "No body"
    }
  }

  @Test
  internal fun `a callback with a binary body still renders the inbox`(spec: RequestSpecification) {
    server.resetRequests()
    // RestAssured directly, as sendCallback takes a String: invalid UTF-8 bytes must reach WireMock as-is
    given()
        .baseUri("http://localhost")
        .port(server.port())
        .contentType("application/octet-stream")
        .body(byteArrayOf(0xC3.toByte(), 0x28, 0x00, 0xFF.toByte()))
        .post("/bin")
        .then()
        .statusCode(200)

    expectThat(inboxPage(spec)) {
      get { getInt("html.body.'**'.find {it.@id == 'callback-count'}.text()") } isEqualTo 1
    }
  }

  @Test
  internal fun `a callback uri keeps its query string`(spec: RequestSpecification) {
    server.resetRequests()
    sendCallback(server, "/hook?a=1&b=2")

    expectThat(inboxPage(spec)) {
      get { getString("html.body.'**'.find {it.@id == 'callback-1-uri'}.text()") } isEqualTo "/hook?a=1&b=2"
    }
  }

  @Test
  internal fun `a callback shows its client ip, body size, headers and query parameters`(spec: RequestSpecification) {
    server.resetRequests()
    sendCallback(server, "/hook?event=ping&id=1&id=2", TEST_JSON_PAYLOAD,
        headers = mapOf("X-Signature" to "sha256=abc", "X-Forwarded-For" to "203.0.113.7"))

    expectThat(inboxPage(spec)) {
      get { getString("html.body.'**'.find {it.@id == 'callback-1-clientIp'}.text()") } isEqualTo "203.0.113.7"
      get { getString("html.body.'**'.find {it.@id == 'callback-1-bodySize'}.text()") } isEqualTo
          "${TEST_JSON_PAYLOAD.length} B"
      get {
        getString("html.body.'**'.find {it.@id == 'callback-1-headers'}.'**'.find {it.th == 'X-Signature'}.td.text()")
      } isEqualTo "sha256=abc"
      get {
        getString("html.body.'**'.find {it.@id == 'callback-1-queryParams'}.'**'.find {it.th == 'id'}.td.text()")
      } isEqualTo "1, 2"
    }
  }

  @Test
  internal fun `a callback without a body or query string hides its size and query parameters`(
      spec: RequestSpecification
  ) {
    server.resetRequests()
    sendCallback(server, "/ping", method = "GET")

    expectThat(inboxPage(spec)) {
      get { getString("html.body.'**'.find {it.@id == 'callback-1-bodySize'}") }.isNull()
      get { getString("html.body.'**'.find {it.@id == 'callback-1-queryParams'}") }.isNull()
    }
  }

  private fun inboxPage(spec: RequestSpecification): XmlPath =
      spec
          .`when`()
          .get(CALLBACK_LOGGER_WEB_URL)
          .then()
          .assertThat()
          .statusCode(200)
          .contentType(ContentType.HTML)
          .extract()
          .body()
          .htmlPath()

  companion object {
    private const val CALLBACK_LOGGER_WEB_URL = "/callback-logger/"
  }
}
