package com.leeturner.callback_logger.adapters.outbound.db

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.leeturner.callback_logger.domain.callbacks.ClearCallbacksError
import com.leeturner.callback_logger.domain.callbacks.ClearCallbacksError.PersistenceError
import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import jakarta.inject.Singleton

@Singleton
class DatabaseCallbackClearerAdapter(
    private val repository: DbCallbackRepository,
) : ClearCallbacksPort {
  override fun clear(): Either<ClearCallbacksError, Unit> = either {
    catch({ repository.deleteAll() }) { e: Exception ->
      raise(PersistenceError(e.message ?: "Unknown persistence error"))
    }
  }
}
