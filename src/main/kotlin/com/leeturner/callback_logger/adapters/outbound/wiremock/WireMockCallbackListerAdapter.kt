package com.leeturner.callback_logger.adapters.outbound.wiremock

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.github.tomakehurst.wiremock.WireMockServer
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.ListCallbacksError
import com.leeturner.callback_logger.domain.callbacks.ListCallbacksError.PersistenceError
import com.leeturner.callback_logger.domain.callbacks.ports.ListCallbacksPort
import jakarta.inject.Singleton

@Singleton
class WireMockCallbackListerAdapter(
    private val server: WireMockServer,
) : ListCallbacksPort {
  // WireMock's journal already lists the most recent request first
  override fun listNewestFirst(): Either<ListCallbacksError, List<Callback>> = either {
    catch({ server.allServeEvents.map { it.toDomain() } }) { e: Exception ->
      raise(PersistenceError(e.message ?: "Unknown persistence error"))
    }
  }
}
