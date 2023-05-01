package com.leeturner.callback_logger.callbacks

import com.leeturner.callback_logger.TestFixtures.TEST_JSON_CALLBACK
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

  @BeforeEach
  fun setup() {
    callbackRepository.deleteAll()
  }

  @Test
  internal fun `callbacks are saved to the database with a default of NEW status`() {
    val savedCallback = callbackRepository.save(TEST_JSON_CALLBACK)

    expectThat(savedCallback) { get { status } isEqualTo CallbackStatus.NEW }
  }

  @Test
  internal fun `callbacks are saved to the database with a timestamp of now`() {
    val savedCallback = callbackRepository.save(TEST_JSON_CALLBACK)

    expectThat(savedCallback) {
      // not the best way of testing this but good enough for now
      get { timestamp } isGreaterThan LocalDateTime.now().minusMinutes(1)
    }
  }

  @Test
  internal fun `callbacks are saved to the database with the correct values`() {
    val savedCallback = callbackRepository.save(TEST_JSON_CALLBACK)

    expectThat(savedCallback) {
      get { httpMethod } isEqualTo TEST_JSON_CALLBACK.httpMethod
      get { httpVersion } isEqualTo TEST_JSON_CALLBACK.httpVersion
      get { payload } isEqualTo TEST_JSON_CALLBACK.payload
    }
  }

  @Test
  internal fun `callbacks are returned ordered by timestamp with the most recent first`() {
    // save two callbacks
    val savedCallback1 = callbackRepository.save(TEST_JSON_CALLBACK)
    val savedCallback2 =
        callbackRepository.save(TEST_JSON_CALLBACK.copy(timestamp = LocalDateTime.now().plusMinutes(10)))

    val orderedCallbacks = callbackRepository.listOrderByTimestampDesc()

    // make sure the callbacks have come back in the correct order
    expectThat(orderedCallbacks)
        .hasSize(2)
        .map(Callback::id)
        .containsExactly(savedCallback2.id, savedCallback1.id)
  }
}
