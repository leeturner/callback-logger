package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.domain.callbacks.Callback
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo

class CurlCommandTest {

  @Test
  internal fun `a post with a body becomes curl with the method, url, headers and data`() {
    val curl =
        callback(
                uri = "/hook?a=1",
                headers =
                    mapOf("Content-Type" to listOf("application/json"), "X-Signature" to listOf("sha256=abc")),
                payload = """{"hello": "world"}""",
            )
            .toCurl(BASE_URL)

    expectThat(curl) isEqualTo
        "curl -X 'POST' 'http://localhost:7071/hook?a=1' \\\n" +
            "  -H 'Content-Type: application/json' \\\n" +
            "  -H 'X-Signature: sha256=abc' \\\n" +
            "  --data-raw '{\"hello\": \"world\"}'"
  }

  @Test
  internal fun `a callback without a body has no data flag`() {
    expectThat(callback(method = "GET", uri = "/ping").toCurl(BASE_URL)) isEqualTo
        "curl -X 'GET' 'http://localhost:7071/ping'"
  }

  @Test
  internal fun `headers curl sets itself are left out whatever their case`() {
    val curl =
        callback(
                headers =
                    mapOf(
                        "Host" to listOf("localhost:7071"),
                        "content-length" to listOf("2"),
                        "CONNECTION" to listOf("keep-alive"),
                        "Accept-Encoding" to listOf("gzip"),
                        "X-Keep" to listOf("1"),
                    ))
            .toCurl(BASE_URL)

    expectThat(curl) {
      not().contains("-H 'Host")
      not().contains("-H 'content-length")
      not().contains("-H 'CONNECTION")
      not().contains("-H 'Accept-Encoding")
      contains("-H 'X-Keep: 1'")
    }
  }

  @Test
  internal fun `a header with several values becomes one -H per value`() {
    expectThat(callback(headers = mapOf("X-Multi" to listOf("a", "b"))).toCurl(BASE_URL)) {
      contains("-H 'X-Multi: a'")
      contains("-H 'X-Multi: b'")
    }
  }

  @Test
  internal fun `a header with an empty value is sent empty`() {
    expectThat(callback(headers = mapOf("X-Empty" to listOf(""))).toCurl(BASE_URL)).contains("-H 'X-Empty;'")
  }

  @Test
  internal fun `single quotes are escaped for the shell`() {
    expectThat(callback(uri = "/it's", payload = """{"name": "it's"}""").toCurl(BASE_URL)) {
      contains("""--data-raw '{"name": "it'\''s"}'""")
      contains("""'http://localhost:7071/it'\''s'""")
    }
  }

  @Test
  internal fun `a multi-line body stays in one argument`() {
    expectThat(callback(payload = "{\n  \"a\": 1\n}").toCurl(BASE_URL)).endsWith("--data-raw '{\n  \"a\": 1\n}'")
  }

  private fun callback(
      method: String = "POST",
      uri: String = "/hook",
      headers: Map<String, List<String>> = emptyMap(),
      payload: String = "",
  ): Callback =
      Callback(
          id = "1",
          timestamp = LocalDateTime.now(),
          uri = uri,
          httpMethod = method,
          httpVersion = "HTTP/1.1",
          payload = payload,
          bodySize = payload.length,
          headers = headers,
          queryParams = emptyMap(),
          clientIp = "127.0.0.1",
      )

  companion object {
    private const val BASE_URL = "http://localhost:7071"
  }
}
