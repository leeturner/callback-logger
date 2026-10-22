package com.leeturner.callback_logger.adapters.outbound.db

import com.leeturner.callback_logger.domain.callbacks.Callback
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.arrow.isRight
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.map

@MicronautTest(environments = [Environment.TEST], startApplication = false)
class DatabaseCallbackAdaptersTest(
    private val lister: DatabaseCallbackListerAdapter,
    private val clearer: DatabaseCallbackClearerAdapter,
    private val repository: DbCallbackRepository,
) {
  @BeforeEach
  fun setup() {
    repository.deleteAll()
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
    repository.save(DbCallback(uri = "/a", httpMethod = "POST", httpVersion = "HTTP_1.1", payload = "1"))
    repository.save(DbCallback(uri = "/b", httpMethod = "POST", httpVersion = "HTTP_1.1", payload = "2"))

    expectThat(clearer.clear()).isRight()
    expectThat(lister.listNewestFirst()).isRight().get { value }.isEmpty()
  }
}
