package com.leeturner.callback_logger.callbacks

import com.leeturner.callback_logger.TestFixtures
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDateTime

class CallbackViewDtoTest {

  private val testCallback =
      CallbackViewDto(
          id = 1L, 
          status = CallbackStatus.NEW.name,
          timestamp = LocalDateTime.parse("2023-04-29T23:41:54.058478"),
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TestFixtures.TEST_JSON_PAYLOAD,
      )
    
  @Test internal fun `timestamps are formatted correctly for the view`() {
      expectThat(testCallback.formattedTimestamp).isEqualTo("29 Apr 2023, 23:41:54")
  }
}
