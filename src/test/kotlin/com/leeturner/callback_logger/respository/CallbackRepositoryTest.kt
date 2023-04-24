package com.leeturner.callback_logger.respository

import com.leeturner.callback_logger.domain.Callback
import com.leeturner.callback_logger.domain.CallbackStatus
import com.leeturner.callback_logger.repository.CallbackRepository
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.map

@MicronautTest(environments = [Environment.TEST], startApplication = false)
class CallbackRepositoryTest(private val callbackRepository: CallbackRepository) {

  private val testPayload =
      """
    {"hello": "world", "foo": "bar"}
  """
          .trimIndent()

  private val testCallback =
      Callback(
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = testPayload,
      )

  @BeforeEach
  fun setup() {
    callbackRepository.deleteAll()
  }

  @Test
  internal fun `callbacks are saved to the database with a default of NEW status`() {
    val savedCallback = callbackRepository.save(testCallback)

    expectThat(savedCallback) { get { status } isEqualTo CallbackStatus.NEW }
  }

  @Test
  internal fun `callbacks are saved to the database with a timestamp of now`() {
    val savedCallback = callbackRepository.save(testCallback)

    expectThat(savedCallback) {
      // not the best way of testing this but good enough for now
      get { timestamp } isGreaterThan LocalDateTime.now().minusMinutes(1)
    }
  }

  @Test
  internal fun `callbacks are saved to the database with the correct values`() {
    val savedCallback = callbackRepository.save(testCallback)

    expectThat(savedCallback) {
      get { httpMethod } isEqualTo testCallback.httpMethod
      get { httpVersion } isEqualTo testCallback.httpVersion
      get { payload } isEqualTo testCallback.payload
    }
  }

  @Test
  internal fun `callbacks saved with timestamps out of order can be returned in order`() {
    // save two callbacks - the second should have a timestamp before the first
    val savedCallback1 = callbackRepository.save(testCallback)
    val savedCallback2 = callbackRepository.save(testCallback.copy(timestamp = LocalDateTime.now().minusMinutes(10)))

    val orderedCallbacks = callbackRepository.listOrderByTimestamp()

    // make sure the callbacks have come back in the correct order
    expectThat(orderedCallbacks)
      .hasSize(2)
      .map(Callback::id)
      .containsExactly(savedCallback2.id, savedCallback1.id)
  }
}
