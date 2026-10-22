package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.adapters.outbound.db.DbCallbackRepository
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_CALLBACK
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.TEST_XML_CALLBACK
import com.leeturner.callback_logger.TestFixtures.TEST_XML_PAYLOAD
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@MicronautTest
class CallbackWebListViewMultipleCallbacksTest(private val callbackRepository: DbCallbackRepository) {

  private val baseDateTime = LocalDateTime.of(2023, 5, 1, 13, 37, 6)
  private val jsonCallbackTimestamp: LocalDateTime = baseDateTime.minusMinutes(10)
  private val xmlCallbackTimestamp: LocalDateTime = baseDateTime.minusMinutes(5)

  @BeforeEach
  fun setup() {
    callbackRepository.deleteAll()
    callbackRepository.save(TEST_JSON_CALLBACK.copy(timestamp = jsonCallbackTimestamp))
    callbackRepository.save(TEST_XML_CALLBACK.copy(timestamp = xmlCallbackTimestamp))
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
    expectThat(callbackRepository.count()) isEqualTo 0L
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
          "HTTP_1.1"
      get { getString("html.body.'**'.find {it.@id == 'callback-1-payload'}.text()") } isEqualTo
          TEST_XML_PAYLOAD
    }

    // Json callback should be next in the list
    expectThat(response.htmlPath()) {
      get { getString("html.body.'**'.find {it.@id == 'callback-2-httpMethod'}.text()") } isEqualTo
          "POST"
      get { getString("html.body.'**'.find {it.@id == 'callback-2-httpVersion'}.text()") } isEqualTo
          "HTTP_1.1"
      get { getString("html.body.'**'.find {it.@id == 'callback-2-payload'}.text()") } isEqualTo
          TEST_JSON_PAYLOAD
    }
  }

  companion object {
    private const val CALLBACK_LOGGER_WEB_URL = "/callback-logger/"
  }
}
