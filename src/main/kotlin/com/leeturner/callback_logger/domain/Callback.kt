package com.leeturner.callback_logger.domain

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime

@Serdeable
@MappedEntity
data class Callback(
    @field:Id @GeneratedValue val id: Long? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: CallbackStatus = CallbackStatus.NEW,
    val httpMethod: String,
    val httpVersion: String,
    @MappedProperty(definition = "CLOB") val payload: String
)
