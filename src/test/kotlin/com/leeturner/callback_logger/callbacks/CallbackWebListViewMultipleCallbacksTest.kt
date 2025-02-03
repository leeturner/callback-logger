package com.leeturner.callback_logger.callbacks

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
import strikt.assertions.isEqualTo

@MicronautTest
class CallbackWebListViewMultipleCallbacksTest(private val callbackRepository: CallbackRepository) {

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
  internal fun `the callback list has an enabled clear all link when there are callbacks`(
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

    val linkPath = "html.body.aside.nav.a.find {it.@id == 'clear-all-link'}"
    expectThat(response.htmlPath()) {
      get { getString("$linkPath.text()").trim() } isEqualTo "Clear All"
    }
  }
  
  @Test
  internal fun `clicking the clear all menu item removes all callbacks`(
    spec: RequestSpecification
  ) {
    val response =
      spec
        .`when`()
        .get("${CALLBACK_LOGGER_WEB_URL}clear/")
        .then()
        .assertThat()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .extract()
        .body()

    val linkPath = "html.body.'**'.find {it.@id == 'callback-count'}"
    expectThat(response.htmlPath()) { get { getInt("$linkPath.text()") } isEqualTo 0 }
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
