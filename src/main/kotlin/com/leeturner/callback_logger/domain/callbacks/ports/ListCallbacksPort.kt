package com.leeturner.callback_logger.domain.callbacks.ports

import arrow.core.Either
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.ListCallbacksError

/**
 * Port for reading every persisted callback.
 */
fun interface ListCallbacksPort {
  /**
   * Returns all callbacks, most recent first.
   * Returns Left on persistence failure.
   */
  fun listNewestFirst(): Either<ListCallbacksError, List<Callback>>
}
