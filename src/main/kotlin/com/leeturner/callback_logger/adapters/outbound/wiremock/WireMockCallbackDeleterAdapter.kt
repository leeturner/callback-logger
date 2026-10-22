package com.leeturner.callback_logger.adapters.outbound.wiremock

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.domain.callbacks.DeleteCallbackError
import com.leeturner.callback_logger.domain.callbacks.DeleteCallbackError.PersistenceError
import com.leeturner.callback_logger.domain.callbacks.ports.DeleteCallbackPort
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class WireMockCallbackDeleterAdapter(
    private val server: WireMockServer,
) : DeleteCallbackPort {
  override fun delete(id: String): Either<DeleteCallbackError, Unit> = either {
    catch({ server.removeServeEvent(UUID.fromString(id)) }) { e: Exception ->
      raise(PersistenceError(e.message ?: "Unknown persistence error"))
    }
  }
}
