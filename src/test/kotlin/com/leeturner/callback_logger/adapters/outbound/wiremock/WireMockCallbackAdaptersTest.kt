package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.TEST_XML_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.sendCallback
import com.leeturner.callback_logger.domain.callbacks.Callback
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.arrow.isRight
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotBlank
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@MicronautTest
class WireMockCallbackAdaptersTest(
    private val server: WireMockServer,
    private val lister: WireMockCallbackListerAdapter,
    private val clearer: WireMockCallbackClearerAdapter,
    private val deleter: WireMockCallbackDeleterAdapter,
    private val watcher: WireMockCallbackWatcherAdapter,
) {

  @BeforeEach
  fun setUp() {
    server.resetRequests()
  }

  @Test
  internal fun `a received callback is listed with its fields`() {
    sendCallback(server, "/api/custom-callback?a=1&b=2", TEST_XML_PAYLOAD, MediaType.APPLICATION_XML, "PUT")

    expectThat(lister.listNewestFirst()).isRight().get { value.single() }.and {
      get { id }.isNotBlank()
      get { uri } isEqualTo "/api/custom-callback?a=1&b=2"
      get { httpMethod } isEqualTo "PUT"
      get { httpVersion } isEqualTo "HTTP/1.1"
      get { payload } isEqualTo TEST_XML_PAYLOAD
      get { timestamp } isGreaterThan LocalDateTime.now().minusMinutes(1)
    }
  }

  @Test
  internal fun `a received callback is listed with its headers, query parameters, client ip and body size`() {
    sendCallback(server, "/hook?event=ping&id=1&id=2", TEST_XML_PAYLOAD, MediaType.APPLICATION_XML,
        // the forwarded ip, as the remote address could be IPv4 or IPv6 loopback
        headers = mapOf("X-Signature" to "sha256=abc", "X-Forwarded-For" to "203.0.113.7"))

    expectThat(lister.listNewestFirst()).isRight().get { value.single() }.and {
      get { headers["X-Signature"] } isEqualTo listOf("sha256=abc")
      get { headers.keys }.contains("Content-Type")
      get { queryParams } isEqualTo mapOf("event" to listOf("ping"), "id" to listOf("1", "2"))
      get { clientIp } isEqualTo "203.0.113.7"
      get { bodySize } isEqualTo TEST_XML_PAYLOAD.length
    }
  }

  @Test
  internal fun `listNewestFirst returns the most recent first`() {
    sendCallback(server, "/first")
    sendCallback(server, "/second")

    expectThat(lister.listNewestFirst())
        .isRight()
        .get { value.map(Callback::uri) }
        .containsExactly("/second", "/first")
  }

  @Test
  internal fun `a long payload is listed whole`() {
    val payload = "x".repeat(10_000)
    sendCallback(server, "/long", payload, MediaType.TEXT_PLAIN)

    expectThat(lister.listNewestFirst()).isRight().get { value.single().payload } isEqualTo payload
  }

  @Test
  internal fun `clear removes every callback and keeps receiving`() {
    sendCallback(server, "/one")
    sendCallback(server, "/two")

    expectThat(clearer.clear()).isRight()
    expectThat(lister.listNewestFirst()).isRight().get { value }.isEmpty()

    sendCallback(server, "/three").statusCode(200)
    expectThat(lister.listNewestFirst()).isRight().get { value }.hasSize(1)
  }

  @Test
  internal fun `delete removes just that callback`() {
    sendCallback(server, "/one")
    sendCallback(server, "/two")
    val newest = server.allServeEvents.first().id.toString()

    expectThat(deleter.delete(newest)).isRight()
    expectThat(lister.listNewestFirst()).isRight().get { value.map(Callback::uri) }.containsExactly("/one")
  }

  @Test
  internal fun `deleting a callback that is already gone succeeds`() {
    sendCallback(server, "/one")

    expectThat(deleter.delete(UUID.randomUUID().toString())).isRight()
    expectThat(lister.listNewestFirst()).isRight().get { value }.hasSize(1)
  }

  @Test
  internal fun `a listener hears about a callback once it is in the journal`() {
    val journalSizes = LinkedBlockingQueue<Int>()
    watcher.onChange { journalSizes.add(server.allServeEvents.size) }

    sendCallback(server, "/callback", TEST_JSON_PAYLOAD).statusCode(200)

    expectThat(journalSizes.poll(5, SECONDS)) isEqualTo 1
  }

  @Test
  internal fun `an admin request does not notify listeners`() {
    val notifications = LinkedBlockingQueue<Unit>()
    watcher.onChange { notifications.add(Unit) }

    sendCallback(server, "/__admin/mappings", method = "GET").statusCode(401)
    sendCallback(server, "/callback", TEST_JSON_PAYLOAD).statusCode(200)

    expectThat(notifications.poll(5, SECONDS)).isNotNull()
    expectThat(notifications.poll(500, MILLISECONDS)).isNull()
  }
}
