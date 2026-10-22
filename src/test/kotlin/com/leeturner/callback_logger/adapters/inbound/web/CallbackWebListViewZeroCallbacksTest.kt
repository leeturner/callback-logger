package com.leeturner.callback_logger.adapters.inbound.web

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@MicronautTest
class CallbackWebListViewZeroCallbacksTest {

  @Test
  internal fun `the callback list view has the correct title`(spec: RequestSpecification) {
    spec
        .`when`()
        .get(CALLBACK_LOGGER_WEB_URL)
        .then()
        .assertThat()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .body("html.head.title", equalTo("Callback Logger - Inbox"))
        .extract()
        .body()
  }

  @Test
  internal fun `the callback list view has the refresh button`(spec: RequestSpecification) {
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

    val buttonPath = "html.body.'**'.find {it.@id == 'refresh-button'}"
    expectThat(response.htmlPath()) {
      get { getString("$buttonPath.text()").trim() } isEqualTo "Refresh"
    }
    // the HTML parser drops attributes with a colon in their name, so check the Datastar action in the raw markup
    expectThat(response.asString()).contains("""data-on:click="@get('/callback-logger/inbox')"""")
  }

  @Test
  internal fun `the callback list has a disabled clear all button when there are no callbacks`(
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
    
    val buttonPath = "html.body.'**'.find {it.@id == 'clear-all-button'}"
    expectThat(response.htmlPath()) {
      get { getString("$buttonPath.text()").trim() } isEqualTo "Clear All"
      get { getString("$buttonPath.@disabled") } isEqualTo "disabled"
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

    val linkPath = "html.body.'**'.find {it.@id == 'github-link'}"
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

  @Test
  internal fun `the callback list view shows the empty inbox message when there are no callbacks`(
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

    val messagePath = "html.body.'**'.find {it.@id == 'empty-inbox'}.p[0]"
    expectThat(response.htmlPath()) {
      get { getString("$messagePath.text()").trim() } isEqualTo "No callbacks yet"
    }
  }

  @Test
  internal fun `the callback list view has the theme toggle`(spec: RequestSpecification) {
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

    val togglePath = "html.body.'**'.find {it.@id == 'theme-toggle'}"
    expectThat(response.htmlPath()) {
      get { getString("$togglePath.@title") } isEqualTo "Switch between the system, light and dark themes"
    }
  }

  @Test
  internal fun `the self hosted css and javascript are served`(spec: RequestSpecification) {
    listOf(
            "bulma/bulma.min.css",
            "datastar/datastar.js",
            "highlightjs/highlight.min.js",
            "highlightjs/github.min.css",
            "highlightjs/github-dark.min.css",
        )
        .forEach { asset ->
          spec.`when`().get("${CALLBACK_LOGGER_WEB_URL}static/vendor/$asset").then().assertThat().statusCode(200)
        }
  }

  companion object {
    private const val CALLBACK_LOGGER_WEB_URL = "/callback-logger/"
  }
}
