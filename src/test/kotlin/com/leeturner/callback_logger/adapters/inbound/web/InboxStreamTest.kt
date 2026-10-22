package com.leeturner.callback_logger.adapters.inbound.web

import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.specification.RequestSpecification
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import java.util.stream.Stream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

@MicronautTest
class InboxStreamTest(
    private val embeddedServer: EmbeddedServer,
    private val wireMock: WireMockServer,
    private val spec: RequestSpecification,
) {
  private val openStreams = mutableListOf<Stream<String>>()

  @BeforeEach
  fun setUp() {
    wireMock.resetRequests()
  }

  @AfterEach
  fun tearDown() {
    openStreams.forEach { it.close() }
  }

  @Test
  internal fun `the stream sends the current inbox as soon as it opens`() {
    val event = nextEvent(openStream())

    expectThat(event) {
      contains("data: mode replace")
      contains("""id="callback-count"""")
      contains("""id="empty-inbox"""")
    }
  }

  @Test
  internal fun `a callback that arrives is pushed down the stream`() {
    val events = openStream()
    nextEvent(events)

    sendCallback(wireMock, "/live-test", TEST_JSON_PAYLOAD).statusCode(200)

    expectThat(nextEvent(events)).contains("/live-test")
  }

  @Test
  internal fun `clearing the inbox is pushed down the stream`() {
    sendCallback(wireMock, "/live-test", TEST_JSON_PAYLOAD).statusCode(200)
    val events = openStream()
    expectThat(nextEvent(events)).contains("/live-test")

    spec.`when`().delete("/callback-logger/inbox").then().statusCode(200)

    expectThat(nextEvent(events)).contains("""id="empty-inbox"""")
  }

  @Test
  internal fun `every line of a multi-line payload is sent as an elements line`() {
    val events = openStream()
    nextEvent(events)

    sendCallback(wireMock, "/multi-line", "{\r\n  \"a\": 1,\n  \"b\": 2\n}").statusCode(200)

    val dataLines = nextEvent(events).lines().filter { it.startsWith("data:") }
    expectThat(dataLines.first()) isEqualTo "data: mode replace"
    expectThat(dataLines.drop(1)).all { startsWith("data: elements ") }
    expectThat(dataLines.joinToString("\n")).contains("&quot;b&quot;: 2")
  }

  // Reads the stream on its own thread and queues each Datastar event, skipping keep-alive comments
  private fun openStream(): BlockingQueue<String> {
    val request = HttpRequest.newBuilder(URI.create("${embeddedServer.uri}/callback-logger/inbox/stream")).build()
    val lines = HttpClient.newHttpClient().send(request, BodyHandlers.ofLines()).body()
    openStreams.add(lines)
    val events = LinkedBlockingQueue<String>()
    Thread.ofVirtual().start {
      val event = StringBuilder()
      runCatching {
        lines.forEach { line ->
          if (line.isEmpty()) {
            if (event.contains("event: datastar-patch-elements")) events.add(event.toString())
            event.clear()
          } else {
            event.appendLine(line)
          }
        }
      }
    }
    return events
  }

  private fun nextEvent(events: BlockingQueue<String>): String = events.poll(5, SECONDS) ?: fail("no event")
}
