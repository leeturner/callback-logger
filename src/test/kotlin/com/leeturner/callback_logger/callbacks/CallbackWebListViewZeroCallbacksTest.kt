package com.leeturner.callback_logger.callbacks

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@MicronautTest
class CallbackWebListViewZeroCallbacksTest() {

  @Test
  internal fun `the callback list view has the correct title`(spec: RequestSpecification) {
    spec
        .`when`()
        .get(CALLBACK_LOGGER_WEB_URL)
        .then()
        .assertThat()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .body("html.head.title", equalTo("Callback Logger - Callbacks Received"))
        .extract()
        .body()
  }

  @Test
  internal fun `the callback list view has the inbox link`(spec: RequestSpecification) {
    val response =
        spec
            .`when`()
            .get(CALLBACK_LOGGER_WEB_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract()
            .body()

    val linkPath = "html.body.aside.nav.a.find {it.@id == 'inbox-link'}"
    expectThat(response.htmlPath()) {
      get { getString("$linkPath.text()").trim() } isEqualTo "Inbox (0)"
    }
  }

  @Test
  internal fun `the callback list view has the github link`(spec: RequestSpecification) {
    val response =
        spec
            .`when`()
            .get(CALLBACK_LOGGER_WEB_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract()
            .body()

    val linkPath = "html.body.aside.a.find {it.@id == 'github-link'}"
    expectThat(response.htmlPath()) {
      get { getString("$linkPath.text()").trim() } isEqualTo "Github"
      get { getString("$linkPath.@href") } isEqualTo "https://github.com/leeturner/callback-logger"
    }
  }

  @Test
  internal fun `the callback list view displays 0 callback count when there are no callbacks saved`(
      spec: RequestSpecification
  ) {
    val response =
        spec
            .`when`()
            .get(CALLBACK_LOGGER_WEB_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract()
            .body()

    val linkPath = "html.body.'**'.find {it.@id == 'callback-count'}"
    expectThat(response.htmlPath()) { get { getInt("$linkPath.text()") } isEqualTo 0 }
  }

  companion object {
    private const val CALLBACK_LOGGER_WEB_URL = "/callback-logger/"
  }
}
