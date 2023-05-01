package com.leeturner.callback_logger.callbacks

import com.leeturner.callback_logger.TestFixtures
import io.micronaut.http.HttpHeaders
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
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
  internal fun `a successful request to the api returns a 200 response`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .and()
        .body(TestFixtures.TEST_JSON_PAYLOAD)
        .`when`()
        .post("/callback-logger/api/callback")
        .then()
        .statusCode(200)
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value =
          [
              "${MediaType.APPLICATION_JSON};${TestFixtures.TEST_JSON_PAYLOAD}",
              "${MediaType.APPLICATION_XML};${TestFixtures.TEST_XML_PAYLOAD}"])
  internal fun `the callback api supports multiple media types and saves the request in the database`(
      mediaType: String,
      payload: String
  ) {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, mediaType)
        .and()
        .body(payload)
        .`when`()
        .post("/callback-logger/api/callback")
        .then()
        .statusCode(200)
  }

  @Test
  internal fun `the callback api and saves the request in the database`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .and()
        .body(TestFixtures.TEST_JSON_PAYLOAD)
        .`when`()
        .post("/callback-logger/api/callback")
        .then()
        .statusCode(200)

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
}
