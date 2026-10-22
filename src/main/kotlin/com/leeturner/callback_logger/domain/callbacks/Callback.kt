package com.leeturner.callback_logger.domain.callbacks

import java.time.LocalDateTime

data class Callback(
    val id: String,
    val timestamp: LocalDateTime,
    val uri: String,
    val httpMethod: String,
    val httpVersion: String,
    val payload: String,
    val bodySize: Int,
    val headers: Map<String, List<String>>,
    val queryParams: Map<String, List<String>>,
    val clientIp: String,
)
