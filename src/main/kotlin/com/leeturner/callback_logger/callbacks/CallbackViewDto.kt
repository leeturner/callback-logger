package com.leeturner.callback_logger.callbacks

import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Serdeable
data class CallbackViewDto(
    val id: Long,
    val status: String,
    val uri: String,
    val timestamp: LocalDateTime,
    val httpMethod: String,
    val httpVersion: String,
    val payload: String,
) {
  val formattedTimestamp: String
    get() = this.timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}
