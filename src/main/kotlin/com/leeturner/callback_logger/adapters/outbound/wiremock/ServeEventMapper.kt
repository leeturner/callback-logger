package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.leeturner.callback_logger.domain.callbacks.Callback
import java.time.LocalDateTime
import java.time.ZoneId

internal fun ServeEvent.toDomain(): Callback =
    Callback(
        id = this.id.toString(),
        timestamp = LocalDateTime.ofInstant(this.request.loggedDate.toInstant(), ZoneId.systemDefault()),
        uri = this.request.url,
        httpMethod = this.request.method.name,
        httpVersion = this.request.protocol,
        payload = this.request.bodyAsString,
    )
