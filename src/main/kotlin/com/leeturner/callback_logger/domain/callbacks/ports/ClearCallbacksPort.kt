package com.leeturner.callback_logger.domain.callbacks.ports

import arrow.core.Either
import com.leeturner.callback_logger.domain.callbacks.ClearCallbacksError

/**
 * Port for deleting every persisted callback.
 */
fun interface ClearCallbacksPort {
  /**
   * Deletes all callbacks.
   * Returns Left on persistence failure.
   */
  fun clear(): Either<ClearCallbacksError, Unit>
}
