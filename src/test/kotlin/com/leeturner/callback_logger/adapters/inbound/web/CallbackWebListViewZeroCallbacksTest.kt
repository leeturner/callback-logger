package com.leeturner.callback_logger.adapters.inbound.web

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@MicronautTest
class CallbackWebListViewZeroCallbacksTest(
    @Property(name = "callback-logger.wiremock.port") private val wireMockPort: Int,
) {

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

    val messagePath = "html.body.'**'.find {it.@id == 'empty-inbox'}.p"
    expectThat(response.htmlPath()) {
      get { getString("$messagePath[0].text()").trim() } isEqualTo "No callbacks yet"
      get { getString("$messagePath[1].text()").trim() } isEqualTo "Send a request and it will show up here."
    }
  }

  @Test
  internal fun `the callback list view opens the live inbox stream`(spec: RequestSpecification) {
    val page = spec.`when`().get(CALLBACK_LOGGER_WEB_URL).then().statusCode(200).extract().asString()

    // the HTML parser drops attributes with a colon in their name, so check the raw markup
    expectThat(page) {
      contains("""data-init="@get('/callback-logger/inbox/stream', {retryMaxCount: Infinity})"""")
      not().contains("auto-refresh")
      not().contains("data-on-interval")
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

  @Test
  internal fun `the empty inbox shows the wiremock callback url`(spec: RequestSpecification) {
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

    val examplePath = "html.body.'**'.find {it.@id == 'empty-inbox'}.pre"
    expectThat(response.htmlPath().getString("$examplePath.text()")) {
      contains("http://localhost:$wireMockPort/callback")
      not().contains("/callback-logger/api/callback")
    }
  }

  companion object {
    private const val CALLBACK_LOGGER_WEB_URL = "/callback-logger/"
  }
}
