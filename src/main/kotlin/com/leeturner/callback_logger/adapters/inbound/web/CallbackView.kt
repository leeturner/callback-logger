package com.leeturner.callback_logger.adapters.inbound.web

import com.leeturner.callback_logger.domain.callbacks.Callback
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Serdeable
data class CallbackView(
    val id: String,
    val uri: String,
    val timestamp: LocalDateTime,
    val httpMethod: String,
    val httpVersion: String,
    val payload: String,
) {
  val formattedTimestamp: String
    get() = this.timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}

fun Callback.toView(): CallbackView =
    CallbackView(
        id = this.id,
        uri = this.uri,
        timestamp = this.timestamp,
        httpMethod = this.httpMethod,
        httpVersion = this.httpVersion,
        payload = this.payload,
    )
