package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.domain.callbacks.Callback

// curl works these out itself for the request it sends, and copying the originals can break it
private val HEADERS_CURL_SETS = setOf("host", "content-length", "connection", "accept-encoding")

// A curl command that sends the callback to the callback logger again, with each argument quoted for the shell
internal fun Callback.toCurl(baseUrl: String): String {
  val headers =
      this.headers
          .filterKeys { it.lowercase() !in HEADERS_CURL_SETS }
          .flatMap { (name, values) ->
            // curl drops a header written as `Name:`, so an empty one is written as `Name;`
            values.map { value -> "-H " + quote(if (value.isEmpty()) "$name;" else "$name: $value") }
          }
  val data = if (this.payload.isEmpty()) emptyList() else listOf("--data-raw " + quote(this.payload))
  // -g, or curl reads [ ] and { } in a query string such as a[0]=1 as patterns for several urls
  return (listOf("curl -g -X ${quote(this.httpMethod)} ${quote(baseUrl + this.uri)}") + headers + data)
      .joinToString(" \\\n  ")
}

private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
