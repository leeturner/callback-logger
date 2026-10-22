package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ServeEventListener
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.leeturner.callback_logger.domain.callbacks.ports.WatchCallbacksPort
import jakarta.inject.Singleton
import java.util.concurrent.CopyOnWriteArrayList

@Singleton
class WireMockCallbackWatcherAdapter : WatchCallbacksPort, ServeEventListener {
  private val listeners = CopyOnWriteArrayList<() -> Unit>()

  override fun onChange(listener: () -> Unit) {
    listeners.add(listener)
  }

  // afterComplete runs once the request is in the journal; addMockServiceRequestListener fires before it is
  override fun afterComplete(serveEvent: ServeEvent, parameters: Parameters) {
    listeners.forEach { it() }
  }

  override fun getName(): String = "callback-watcher"
}
