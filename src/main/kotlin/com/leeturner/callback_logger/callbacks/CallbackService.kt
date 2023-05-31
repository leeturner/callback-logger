package com.leeturner.callback_logger.callbacks

import jakarta.inject.Singleton

@Singleton
class CallbackService(
    private val callbackRepository: CallbackRepository,
) {
  fun saveCallback(uri: String, httpMethod: String, httpVersion: String, body: String) {
    val callback = Callback(uri = uri, httpMethod = httpMethod, httpVersion = httpVersion, payload = body)
    callbackRepository.save(callback)
  }

  fun getCallbacksOrderedByTimestampDesc(): List<CallbackViewDto> {
    return callbackRepository.listOrderByTimestampDesc().map { callback: Callback ->
      callback.toDto()
    }
  }

  fun clearAllCallbacks() {
    callbackRepository.deleteAll()
  }

  private fun Callback.toDto(): CallbackViewDto {
    return CallbackViewDto(
      id = this.id ?: 0,
      status = this.status.name,
      uri = this.uri,
      timestamp = this.timestamp,
      httpMethod = this.httpMethod,
      httpVersion = this.httpVersion,
      payload = this.payload
    )
  }
}
