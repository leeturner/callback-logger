package com.leeturner.callback_logger.callbacks

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("callback-logger.custom-callbacks")
data class CustomCallbackConfiguration (
  val post: List<String>,
  val put: List<String>,
)
