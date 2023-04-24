package com.leeturner.callback_logger.api.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CallbackDto(
    val id: Long,
    val timestamp: String,
    val httpMethod: String,
    val httpVersion: String,
    val payload: String,
)
