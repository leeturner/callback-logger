package com.leeturner.callback_logger.domain.callbacks

sealed interface DeleteCallbackError {
  data class PersistenceError(
      val message: String,
  ) : DeleteCallbackError
}
