package com.leeturner.callback_logger.adapters.outbound.db

import com.leeturner.callback_logger.TestFixtures.TEST_JSON_PAYLOAD
import com.leeturner.callback_logger.domain.callbacks.Callback
import com.leeturner.callback_logger.domain.callbacks.CallbackStatus
import com.leeturner.callback_logger.domain.callbacks.NewCallback
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.arrow.isRight
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.map
import strikt.assertions.single

@MicronautTest(environments = [Environment.TEST], startApplication = false)
class DatabaseCallbackAdaptersTest(
    private val saver: DatabaseCallbackSaverAdapter,
    private val lister: DatabaseCallbackListerAdapter,
    private val clearer: DatabaseCallbackClearerAdapter,
    private val repository: DbCallbackRepository,
) {
  private val newCallback =
      NewCallback(
          uri = "/api/callback",
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TEST_JSON_PAYLOAD,
      )

  @BeforeEach
  fun setup() {
    repository.deleteAll()
  }

  @Test
  internal fun `save stores a callback with NEW status`() {
    expectThat(saver.save(newCallback)).isRight().and {
      get { value.status } isEqualTo CallbackStatus.NEW
      get { value.id } isGreaterThan 0L
    }
  }

  @Test
  internal fun `save stores a callback with a timestamp of now`() {
    expectThat(saver.save(newCallback)).isRight().get { value.timestamp } isGreaterThan
        LocalDateTime.now().minusMinutes(1)
  }

  @Test
  internal fun `save stores the callback values`() {
    expectThat(saver.save(newCallback)).isRight().and {
      get { value.uri } isEqualTo newCallback.uri
      get { value.httpMethod } isEqualTo newCallback.httpMethod
      get { value.httpVersion } isEqualTo newCallback.httpVersion
      get { value.payload } isEqualTo newCallback.payload
    }
  }

  @Test
  internal fun `save stores payloads longer than a varchar`() {
    val payload = "x".repeat(10_000)

    expectThat(saver.save(newCallback.copy(payload = payload))).isRight()

    expectThat(lister.listNewestFirst()).isRight().get { value }.single().get { this.payload } isEqualTo payload
  }

  @Test
  internal fun `listNewestFirst returns the most recent first`() {
    // seeded through the repository because only it can set the timestamp
    val first = repository.save(DbCallback(uri = "/a", httpMethod = "POST", httpVersion = "HTTP_1.1", payload = "1"))
    val second =
        repository.save(
            DbCallback(
                timestamp = LocalDateTime.now().plusMinutes(10),
                uri = "/b",
                httpMethod = "POST",
                httpVersion = "HTTP_1.1",
                payload = "2",
            ))

    expectThat(lister.listNewestFirst())
        .isRight()
        .get { value }
        .map(Callback::id)
        .containsExactly(second.id!!, first.id!!)
  }

  @Test
  internal fun `clear removes every callback`() {
    saver.save(newCallback)
    saver.save(newCallback)

    expectThat(clearer.clear()).isRight()
    expectThat(lister.listNewestFirst()).isRight().get { value }.isEmpty()
  }
}
