package com.leeturner.callback_logger.adapters.outbound.wiremock

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.domain.callbacks.ClearCallbacksError
import com.leeturner.callback_logger.domain.callbacks.ClearCallbacksError.PersistenceError
import com.leeturner.callback_logger.domain.callbacks.ports.ClearCallbacksPort
import jakarta.inject.Singleton

@Singleton
class WireMockCallbackClearerAdapter(
    private val server: WireMockServer,
) : ClearCallbacksPort {
  // clears the journal only; the catch-all stub stays, so callbacks keep arriving
  override fun clear(): Either<ClearCallbacksError, Unit> = either {
    catch({ server.resetRequests() }) { e: Exception ->
      raise(PersistenceError(e.message ?: "Unknown persistence error"))
    }
  }
}
