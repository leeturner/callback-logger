package com.leeturner.callback_logger.callbacks

import com.leeturner.callback_logger.TestFixtures
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

class CallbackViewDtoTest {

  private val testCallback =
      CallbackViewDto(
          id = 1L,
          status = CallbackStatus.NEW.name,
          uri = "/api/callback",
          timestamp = LocalDateTime.parse("2023-04-29T23:41:54.058478"),
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TestFixtures.TEST_JSON_PAYLOAD,
      )

  @Test
  internal fun `timestamps are formatted correctly for the view`() {
    expectThat(testCallback.formattedTimestamp).contains("29 Apr 2023")
  }
}
