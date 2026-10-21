package com.leeturner.callback_logger.domain.callbacks.ports

import arrow.core.Either
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.NewCallback
import com.leeturner.callback_logger.domain.callbacks.SaveCallbackError

/**
 * Port for persisting a received callback.
 */
fun interface SaveCallbackPort {
  /**
   * Saves the callback with a NEW status and the current time, returning what was stored.
   * Returns Left on persistence failure.
   */
  fun save(newCallback: NewCallback): Either<SaveCallbackError, Callback>
}
