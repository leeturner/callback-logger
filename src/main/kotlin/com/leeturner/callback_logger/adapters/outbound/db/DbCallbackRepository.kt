package com.leeturner.callback_logger.adapters.outbound.db

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface DbCallbackRepository : CrudRepository<DbCallback, Long> {
    fun listOrderByTimestampDesc(): List<DbCallback>
}
