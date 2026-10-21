package com.leeturner.callback_logger.adapters.outbound.db

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.NewCallback
import com.leeturner.callback_logger.domain.callbacks.SaveCallbackError
import com.leeturner.callback_logger.domain.callbacks.SaveCallbackError.PersistenceError
import com.leeturner.callback_logger.domain.callbacks.ports.SaveCallbackPort
import jakarta.inject.Singleton

@Singleton
class DatabaseCallbackSaverAdapter(
    private val repository: DbCallbackRepository,
) : SaveCallbackPort {
  override fun save(newCallback: NewCallback): Either<SaveCallbackError, Callback> = either {
    catch({
      repository
          .save(
              DbCallback(
                  uri = newCallback.uri,
                  httpMethod = newCallback.httpMethod,
                  httpVersion = newCallback.httpVersion,
                  payload = newCallback.payload,
              ))
          .toDomain()
    }) { e: Exception ->
      raise(PersistenceError(e.message ?: "Unknown persistence error"))
    }
  }
}
