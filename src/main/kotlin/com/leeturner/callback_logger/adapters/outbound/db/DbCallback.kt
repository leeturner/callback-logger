package com.leeturner.callback_logger.adapters.outbound.db

import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.CallbackStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import java.time.LocalDateTime

@MappedEntity("callback")
data class DbCallback(
    @field:Id @GeneratedValue val id: Long? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: CallbackStatus = CallbackStatus.NEW,
    val uri: String,
    val httpMethod: String,
    val httpVersion: String,
    @MappedProperty(definition = "CLOB") val payload: String
)

internal fun DbCallback.toDomain(): Callback =
    Callback(
        id = requireNotNull(this.id) { "DbCallback.id must be set; called toDomain() on an unsaved entity?" },
        timestamp = this.timestamp,
        status = this.status,
        uri = this.uri,
        httpMethod = this.httpMethod,
        httpVersion = this.httpVersion,
        payload = this.payload,
    )
