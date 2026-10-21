package com.leeturner.callback_logger.domain.callbacks

sealed interface ClearCallbacksError {
  data class PersistenceError(
      val message: String,
  ) : ClearCallbacksError
}
