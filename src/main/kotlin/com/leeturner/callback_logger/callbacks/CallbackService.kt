package com.leeturner.callback_logger.callbacks

import arrow.core.getOrElse
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.NewCallback
import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import com.leeturner.callback_logger.domain.callbacks.ports.SaveCallbackPort
import jakarta.inject.Singleton

@Singleton
class CallbackService(
    private val saveCallbackPort: SaveCallbackPort,
    private val listCallbacksPort: ListCallbacksPort,
    private val clearCallbacksPort: ClearCallbacksPort,
) {
  fun saveCallback(uri: String, httpMethod: String, httpVersion: String, body: String) {
    saveCallbackPort
        .save(NewCallback(uri = uri, httpMethod = httpMethod, httpVersion = httpVersion, payload = body))
        .getOrElse { throw IllegalStateException(it.toString()) }
  }

  fun getCallbacksOrderedByTimestampDesc(): List<CallbackViewDto> {
    return listCallbacksPort.listNewestFirst().getOrElse { throw IllegalStateException(it.toString()) }.map {
        callback: Callback ->
      callback.toDto()
    }
  }

  fun clearAllCallbacks() {
    clearCallbacksPort.clear().getOrElse { throw IllegalStateException(it.toString()) }
  }

  private fun Callback.toDto(): CallbackViewDto {
    return CallbackViewDto(
      id = this.id,
      status = this.status.name,
      uri = this.uri,
      timestamp = this.timestamp,
      httpMethod = this.httpMethod,
      httpVersion = this.httpVersion,
      payload = this.payload
    )
  }
}
