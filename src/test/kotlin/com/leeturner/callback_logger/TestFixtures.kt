package com.leeturner.callback_logger

import com.leeturner.callback_logger.adapters.outbound.db.DbCallback

object TestFixtures {
  const val CALLBACK_LOGGER_API_URL = "/callback-logger/api/callback"

  const val TEST_JSON_PAYLOAD = """{"hello": "world", "foo": "bar"}"""
  const val TEST_XML_PAYLOAD = """<a><test>XML Payload</test></a>"""

  val TEST_JSON_CALLBACK =
      DbCallback(
          uri = "/api/callback",
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TEST_JSON_PAYLOAD,
      )
  val TEST_XML_CALLBACK =
      DbCallback(
          uri = "/api/custom-callback",
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TEST_XML_PAYLOAD,
      )
}
