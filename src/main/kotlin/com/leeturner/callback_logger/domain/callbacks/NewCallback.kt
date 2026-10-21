package com.leeturner.callback_logger.domain.callbacks

data class NewCallback(
    val uri: String,
    val httpMethod: String,
    val httpVersion: String,
    val payload: String,
)
