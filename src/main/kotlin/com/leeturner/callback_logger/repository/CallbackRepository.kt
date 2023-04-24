package com.leeturner.callback_logger.repository

import com.leeturner.callback_logger.domain.Callback
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface CallbackRepository : CrudRepository<Callback, Long> {
    fun listOrderByTimestamp(): List<Callback>
}