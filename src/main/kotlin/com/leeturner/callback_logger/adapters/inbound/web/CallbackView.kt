package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.domain.callbacks.Callback
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Serdeable
data class CallbackView(
    val id: String,
    val uri: String,
    val timestamp: LocalDateTime,
    val httpMethod: String,
    val httpVersion: String,
    val payload: String,
    val bodySize: Int,
    val headers: Map<String, List<String>>,
    val queryParams: Map<String, List<String>>,
    val clientIp: String,
) {
  val formattedTimestamp: String
    get() = this.timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))

  val formattedBodySize: String
    get() =
        when {
          this.bodySize < 1024 -> "${this.bodySize} B"
          this.bodySize < 1024 * 1024 -> "%.1f KB".format(Locale.ROOT, this.bodySize / 1024.0)
          else -> "%.1f MB".format(Locale.ROOT, this.bodySize / (1024.0 * 1024))
        }
}

fun Callback.toView(): CallbackView =
    CallbackView(
        id = this.id,
        uri = this.uri,
        timestamp = this.timestamp,
        httpMethod = this.httpMethod,
        httpVersion = this.httpVersion,
        payload = this.payload,
        bodySize = this.bodySize,
        headers = this.headers,
        queryParams = this.queryParams,
        clientIp = this.clientIp,
    )
