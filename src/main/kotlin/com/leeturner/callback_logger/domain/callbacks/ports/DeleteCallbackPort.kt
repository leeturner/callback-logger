package com.leeturner.callback_logger.domain.callbacks.ports

import arrow.core.Either
import com.leeturner.callback_logger.domain.callbacks.DeleteCallbackError

/**
 * Port for deleting a single persisted callback.
 */
fun interface DeleteCallbackPort {
  /**
   * Deletes the callback with the given id. Deleting a callback that is already gone succeeds.
   * Returns Left on persistence failure.
   */
  fun delete(id: String): Either<DeleteCallbackError, Unit>
}
