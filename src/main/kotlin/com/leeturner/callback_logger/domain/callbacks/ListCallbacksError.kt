package com.leeturner.callback_logger.domain.callbacks

sealed interface ListCallbacksError {
  data class PersistenceError(
      val message: String,
  ) : ListCallbacksError
}
