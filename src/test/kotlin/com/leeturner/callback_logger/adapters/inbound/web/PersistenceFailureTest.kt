package com.leeturner.callback_logger.adapters.inbound.web

import arrow.core.left
import com.leeturner.callback_logger.domain.callbacks.ClearCallbacksError
import com.leeturner.callback_logger.domain.callbacks.DeleteCallbackError
import com.leeturner.callback_logger.domain.callbacks.ListCallbacksError
import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.DeleteCallbackPort
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.runtime.server.EmbeddedServer
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.UUID
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@MicronautTest
class PersistenceFailureTest {

  @Inject lateinit var spec: RequestSpecification

  @Inject lateinit var embeddedServer: EmbeddedServer

  @MockBean(ListCallbacksPort::class)
  fun failingList() = ListCallbacksPort { ListCallbacksError.PersistenceError("boom").left() }

  @MockBean(ClearCallbacksPort::class)
  fun failingClear() = ClearCallbacksPort { ClearCallbacksError.PersistenceError("boom").left() }

  @MockBean(DeleteCallbackPort::class)
  fun failingDelete() = DeleteCallbackPort { DeleteCallbackError.PersistenceError("boom").left() }

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

  @Test
  internal fun `delete returns 500 when a callback cannot be deleted`() {
    spec.`when`().delete("/callback-logger/inbox/${UUID.randomUUID()}").then().statusCode(500)
  }

  @Test
  internal fun `the inbox stream returns 500 when callbacks cannot be loaded`() {
    val request =
        HttpRequest.newBuilder(URI.create("${embeddedServer.uri}/callback-logger/inbox/stream"))
            .timeout(Duration.ofSeconds(5))
            .build()

    expectThat(HttpClient.newHttpClient().send(request, BodyHandlers.ofString()).statusCode()) isEqualTo 500
  }
}
