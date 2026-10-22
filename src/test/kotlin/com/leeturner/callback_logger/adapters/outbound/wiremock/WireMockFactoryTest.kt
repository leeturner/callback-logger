package com.leeturner.callback_logger.adapters.outbound.wiremock

import com.leeturner.callback_logger.TestFixtures.sendCallback
import io.micronaut.http.HttpStatus
import java.nio.file.Files
import java.nio.file.Path
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

class WireMockFactoryTest {

  @Test
  internal fun `stub files in the folder the app is started from do not replace the catch-all response`() {
    // WireMock's default files root is src/test/resources, relative to the working directory
    val mappings = Path.of("src/test/resources/mappings")
    val createdMappings = Files.notExists(mappings)
    val stubFile = mappings.resolve("callback-logger-override.json")
    Files.createDirectories(mappings)
    Files.writeString(
        stubFile,
        """{"priority": 1, "request": {"method": "ANY", "urlPattern": ".*"}, "response": {"status": 418}}""")

    val server = WireMockFactory().wireMockServer(0, HttpStatus.OK, "", "", 1000, WireMockCallbackWatcherAdapter())
    try {
      sendCallback(server, "/callback")
          .statusCode(200)
          .contentType("application/json")
          .body(equalTo(""))
    } finally {
      server.stop()
      Files.delete(stubFile)
      if (createdMappings) Files.delete(mappings)
    }
  }
}
