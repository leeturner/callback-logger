package com.leeturner.callback_logger.callbacks

import jakarta.inject.Singleton

@Singleton
class CallbackService(
  private val callbackRepository: CallbackRepository,
) {
  fun saveCallback(httpMethod: String, httpVersion: String, body: String) {
    val callback = Callback(httpMethod = httpMethod, httpVersion = httpVersion, payload = body)
    callbackRepository.save(callback)
  }

  fun getCallbacksOrderedByTimestampDesc(): List<CallbackViewDto> {
    return callbackRepository.listOrderByTimestampDesc().map { callback: Callback ->
      callback.toDto()
    }
  }

  private fun Callback.toDto(): CallbackViewDto {
    return CallbackViewDto(
        this.id ?: 0, this.status.name, this.timestamp, this.httpMethod, this.httpVersion, this.payload)
  }

}
