package com.crownagentsbank.callback.api

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post

@Controller("/api")
class CallbackController {
    @Post("/callback")
    fun callback(@Body body: String) {
        println(body)
    }
}