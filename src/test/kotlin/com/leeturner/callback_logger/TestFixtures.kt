package com.leeturner.callback_logger

import com.leeturner.callback_logger.callbacks.Callback

object TestFixtures {

  const val TEST_JSON_PAYLOAD = """{"hello": "world", "foo": "bar"}"""
  const val TEST_XML_PAYLOAD = """<a><test>XML Payload</test></a>"""

  val TEST_JSON_CALLBACK =
      Callback(
          uri = "/api/callback",
          httpMethod = "POST",
          httpVersion = "HTTP_1.1",
          payload = TEST_JSON_PAYLOAD,
      )
    val TEST_XML_CALLBACK =
        Callback(
            uri = "/api/custom-callback",
            httpMethod = "POST",
            httpVersion = "HTTP_1.1",
            payload = TEST_XML_PAYLOAD,
        )
}
