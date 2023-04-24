package com.leeturner.callback_logger

import org.h2.tools.Server
import io.micronaut.runtime.Micronaut.run

fun main(args: Array<String>) {
  // this is just used for development purposes to start the h2 console.
  // can be removed.  The console can be accessed on http://localhost:8082/
  Server.createWebServer().start()
  run(*args)
}
