package com.leeturner.callback_logger.callbacks

import com.leeturner.callback_logger.TestFixtures
import io.micronaut.http.HttpHeaders
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize

@MicronautTest
class CustomCallbackTests(
    private val callbackRepository: CallbackRepository,
    private val customCallbackRoutes: CustomCallbackRoutes,
    private val customCallbackConfiguration: CustomCallbackConfiguration,
) {

  @Inject lateinit var spec: RequestSpecification

  @BeforeEach
  fun setUp() {
    callbackRepository.deleteAll()
  }

  @Test
  internal fun `duplicate custom callbacks are removed before registering`() {
    // there are 5 callbacks defined in the application-test.conf file and two of those are
    // duplicates.
    expectThat(customCallbackConfiguration.post + customCallbackConfiguration.put).hasSize(5)

    // As a result there should only be 3 callbacks registered
    expectThat(customCallbackRoutes.uriRoutes).hasSize(3)
  }

  @Test
  internal fun `a successful post request to the custom endpoint returns a 200 response`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .and()
        .body(TestFixtures.TEST_JSON_PAYLOAD)
        .`when`()
        .post("/api/custom1")
        .then()
        .statusCode(200)
  }

  @Test
  internal fun `a successful put request to the custom endpoint returns a 200 response`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .and()
        .body(TestFixtures.TEST_XML_PAYLOAD)
        .`when`()
        .put("/api/custom1")
        .then()
        .statusCode(200)
  }
}
