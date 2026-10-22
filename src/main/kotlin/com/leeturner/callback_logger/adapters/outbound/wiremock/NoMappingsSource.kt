package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.github.tomakehurst.wiremock.core.MappingsSaver
import com.github.tomakehurst.wiremock.standalone.MappingsSource
import com.github.tomakehurst.wiremock.stubbing.StubMappings

/**
 * Loads and saves no stub files. WireMock otherwise loads them from src/test/resources/mappings under the working
 * directory, which could replace the catch-all response depending on where the app is started.
 */
internal object NoMappingsSource : MappingsSource, MappingsSaver by MappingsSaver.NOOP {
  override fun loadMappingsInto(stubMappings: StubMappings) {}
}
