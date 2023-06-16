package com.leeturner.callback_logger.callbacks

import com.leeturner.callback_logger.TestFixtures
import com.leeturner.callback_logger.TestFixtures.CALLBACK_LOGGER_API_URL
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpHeaders
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@MicronautTest(environments = ["payload"])
class CallbackApiResponsePayloadTests(
    private val callbackRepository: CallbackRepository,
    @Property(name = "callback-logger.callback-response-payload")
    private val callbackResponsePayload: String,
) {

  @Inject lateinit var spec: RequestSpecification

  @BeforeEach
  fun setUp() {
    callbackRepository.deleteAll()
  }

  @Test
  internal fun `a successful post request to the api returns the custom configured status code`() {
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
        .body(equalTo(callbackResponsePayload))
  }

  @Test
  internal fun `a successful put request to the api returns the custom configured status code`() {
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
        .body(equalTo(callbackResponsePayload))
  }
}
