package com.leeturner.callback_logger.adapters.inbound.web

import arrow.core.left
import com.leeturner.callback_logger.TestFixtures.CALLBACK_LOGGER_API_URL
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.domain.callbacks.ClearCallbacksError
import com.leeturner.callback_logger.domain.callbacks.ListCallbacksError
import com.leeturner.callback_logger.domain.callbacks.SaveCallbackError
import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.SaveCallbackPort
import io.micronaut.http.HttpHeaders
import io.micronaut.http.MediaType
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test

@MicronautTest
class PersistenceFailureTest {

  @Inject lateinit var spec: RequestSpecification

  @MockBean(SaveCallbackPort::class)
  fun failingSave() = SaveCallbackPort { SaveCallbackError.PersistenceError("boom").left() }

  @MockBean(ListCallbacksPort::class)
  fun failingList() = ListCallbacksPort { ListCallbacksError.PersistenceError("boom").left() }

  @MockBean(ClearCallbacksPort::class)
  fun failingClear() = ClearCallbacksPort { ClearCallbacksError.PersistenceError("boom").left() }

  @Test
  internal fun `saving a callback that fails to persist returns 500`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .body(TEST_JSON_PAYLOAD)
        .`when`()
        .post(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(500)
  }

  @Test
  internal fun `a put callback that fails to persist returns 500`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .body(TEST_JSON_PAYLOAD)
        .`when`()
        .put(CALLBACK_LOGGER_API_URL)
        .then()
        .statusCode(500)
  }

  @Test
  internal fun `a custom route callback that fails to persist returns 500`() {
    spec
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .body(TEST_JSON_PAYLOAD)
        .`when`()
        .post("/api/custom1")
        .then()
        .statusCode(500)
  }

  @Test
  internal fun `the inbox page returns 500 when callbacks cannot be loaded`() {
    spec.`when`().get("/callback-logger/").then().statusCode(500)
  }

  @Test
  internal fun `the inbox page does not render an empty inbox when callbacks cannot be loaded`() {
    spec.`when`().get("/callback-logger/").then().body(not(containsString("Callback Logger - Inbox")))
  }

  @Test
  internal fun `refreshing the inbox returns 500 when callbacks cannot be loaded`() {
    spec.`when`().get("/callback-logger/inbox").then().statusCode(500)
  }

  @Test
  internal fun `clear returns 500 when callbacks cannot be cleared`() {
    spec.`when`().delete("/callback-logger/inbox").then().statusCode(500)
  }
}
