package com.leeturner.callback_logger.adapters.outbound.db

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.ListCallbacksError
import com.leeturner.callback_logger.domain.callbacks.ListCallbacksError.PersistenceError
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import jakarta.inject.Singleton

@Singleton
class DatabaseCallbackListerAdapter(
    private val repository: DbCallbackRepository,
) : ListCallbacksPort {
  override fun listNewestFirst(): Either<ListCallbacksError, List<Callback>> = either {
    catch({ repository.listOrderByTimestampDesc().map { it.toDomain() } }) { e: Exception ->
      raise(PersistenceError(e.message ?: "Unknown persistence error"))
    }
  }
}
