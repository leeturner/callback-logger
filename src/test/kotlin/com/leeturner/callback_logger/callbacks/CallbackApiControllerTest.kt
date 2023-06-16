package com.leeturner.callback_logger.callbacks

import com.leeturner.callback_logger.TestFixtures
import io.micronaut.http.HttpHeaders
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@MicronautTest
class CallbackApiControllerTest(private val callbackRepository: CallbackRepository) {

  @Inject lateinit var spec: RequestSpecification

  @BeforeEach
  fun setUp() {
    callbackRepository.deleteAll()
  }

  @Test
  internal fun `a successful post request to the api returns a 200 response`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .and()
        .body(TestFixtures.TEST_JSON_PAYLOAD)
        .`when`()
        .post(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(200)
        .and()
        .body(equalTo(""))
  }

  @Test
  internal fun `a successful put request to the api returns a 200 response`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .and()
        .body(TestFixtures.TEST_JSON_PAYLOAD)
        .`when`()
        .put(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(200)
        .and()
        .body(equalTo(""))
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value =
          [
              "${MediaType.APPLICATION_JSON};${TestFixtures.TEST_JSON_PAYLOAD}",
              "${MediaType.APPLICATION_XML};${TestFixtures.TEST_XML_PAYLOAD}"])
  internal fun `the post callback api supports multiple media types and saves the request in the database`(
      mediaType: String,
      payload: String
  ) {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, mediaType)
        .and()
        .body(payload)
        .`when`()
        .post(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(200)
        .and()
        .body(equalTo(""))
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value =
          [
              "${MediaType.APPLICATION_JSON};${TestFixtures.TEST_JSON_PAYLOAD}",
              "${MediaType.APPLICATION_XML};${TestFixtures.TEST_XML_PAYLOAD}"])
  internal fun `the put callback api supports multiple media types and saves the request in the database`(
      mediaType: String,
      payload: String
  ) {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, mediaType)
        .and()
        .body(payload)
        .`when`()
        .put(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(200)
        .and()
        .body(equalTo(""))
  }

  @Test
  internal fun `the post callback api and saves the request in the database`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .and()
        .body(TestFixtures.TEST_JSON_PAYLOAD)
        .`when`()
        .post(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(200)
        .and()
        .body(equalTo(""))

    val callbacks = callbackRepository.findAll().toList()
    expectThat(callbacks).hasSize(1)

    val callback = callbacks[0]
    expectThat(callback) {
      get { httpMethod } isEqualTo "POST"
      get { httpVersion } isEqualTo "HTTP_1_1"
      get { status } isEqualTo CallbackStatus.NEW
      get { payload } isEqualTo TestFixtures.TEST_JSON_PAYLOAD
    }
  }

  @Test
  internal fun `the put callback api and saves the request in the database`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .and()
        .body(TestFixtures.TEST_JSON_PAYLOAD)
        .`when`()
        .put(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(200)
        .and()
        .body(equalTo(""))

    val callbacks = callbackRepository.findAll().toList()
    expectThat(callbacks).hasSize(1)

    val callback = callbacks[0]
    expectThat(callback) {
      get { httpMethod } isEqualTo "PUT"
      get { httpVersion } isEqualTo "HTTP_1_1"
      get { status } isEqualTo CallbackStatus.NEW
      get { payload } isEqualTo TestFixtures.TEST_JSON_PAYLOAD
    }
  }

  companion object {
    private const val CALLBACK_LOGGER_API_URL = "/callback-logger/api/callback"
  }
}
