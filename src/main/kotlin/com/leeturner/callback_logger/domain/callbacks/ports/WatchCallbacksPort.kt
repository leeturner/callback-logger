package com.leeturner.callback_logger.domain.callbacks.ports

/**
 * Port for hearing about new callbacks as they arrive.
 */
fun interface WatchCallbacksPort {
  /**
   * Calls [listener] after each new callback is stored, on the thread that received it.
   */
  fun onChange(listener: () -> Unit)
}
