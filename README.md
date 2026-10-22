# callback-logger

![buildStatus](https://img.shields.io/github/workflow/status/leeturner/callback-logger/Java%20CI%20with%20Gradle?style=plastic)
![latestVersion](https://img.shields.io/github/v/release/leeturner/callback-logger)
<a href="https://twitter.com/leeturner" target="_blank">
<img alt="Twitter: leeturner" src="https://img.shields.io/twitter/follow/leeturner.svg?style=social" />
</a>

> Simple service to receive callbacks. Useful for when testing APIs locally that send callbacks

## 🦿 Prerequisites

- Java 25 or above (callback-logger runs on the JVM only; a GraalVM native image is not supported)

## 🛠 Installation

1. Download latest sources and run:
 
 ```shell script
./gradlew build run
```

## ⌨️ Usage

Callbacks are received by an embedded [WireMock](https://wiremock.org/) server on its own port, `7071`, while the
web interface stays on `7070`. Point the API you are testing at **any path** on the callback port, with **any HTTP
method**, for example `http://localhost:7071/callback` or `http://localhost:7071/orders/42/status`. Every request is
recorded, whatever its path, method or content type. You can send a callback manually using the following `curl`
request:

```shell
curl -X POST --location "http://localhost:7071/callback" \
    -H "Content-Type: application/json" \
    -d "{\"hello\": \"world\"}"
```

or

```shell
curl -X PATCH --location "http://localhost:7071/orders/42/status" \
    -H "Content-Type: application/xml" \
    -d "<hello>world</hello>" 
```

More examples, including form posts, query strings and custom methods such as `PROPFIND`, are in
[`src/test/resources/http/callbacks.http`](src/test/resources/http/callbacks.http).

> **Breaking change:** callbacks used to be sent to `http://localhost:7070/callback-logger/api/callback`, plus any
> custom callback URIs configured under `callback-logger.custom-callbacks`. Both are gone: send callbacks to the
> callback port instead. The same paths still work there, only the port has changed.

Paths under `/__admin` are reserved by WireMock. Its admin API is locked, so those requests are refused with a `401`
and are not recorded as callbacks.

### Change the callback port

The callback port is set in the `application.conf` file:

```hocon
callback-logger {
  wiremock {
    port = 7071
  }
}
```

### Specify the http response code

By default, callback-logger returns a `200` response code for all callbacks.  If you need to return a different response
code then you can define the response code in the `application.conf` file.  For example:

```hocon
callback-logger {
  callback-response-status-code = 201
}
```

The application will fail to start if the response code is not a valid http response code.

### Specify the http response payload

By default, callback-logger returns an empty string as the response payload for all callbacks, with a
`Content-Type: application/json` header.  If you need to return a different response payload then you can change that
in the `application.conf` file.  For example:

```hocon
callback-logger {
  callback-response-payload = "{\"status\": \"OK\", \"message\": \"Callback received\"}"
}
```

### Web UI

`callback-logger` provides a simple web interface to allow you to see the callbacks you have received.  You can access
the web interface by pointing your browser to `http://localhost:7070/callback-logger`.

Press **Refresh** (or tick **Auto-refresh** to check every 5 seconds) to pull in new callbacks, and **Clear All** to
empty the inbox. Both update the page in place using [Datastar](https://data-star.dev/), without a full page reload.
The page is styled with [Bulma](https://bulma.io/), and Bulma, Datastar and highlight.js are all served by the app
itself, so the inbox works with no internet connection:


![](docs/callback-logger-ui.png)


## 🥼 Run tests

```shell script
./gradlew test
```

## ✍️ Author

👤 **Lee Turner**

* Mastodon: <a href="https://hachyderm.io/@leeturner" target="_blank">@leeturner</a>
* Twitter: <a href="https://twitter.com/leeturner" target="_blank">@leeturner</a>

Feel free to ping me 😉

## 🤝 Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any
contributions you make are **greatly appreciated**.

1. Open an issue first to discuss what you would like to change.
1. Fork the Project
1. Create your feature branch (`git checkout -b feature/amazing-feature`)
1. Commit your changes (`git commit -m 'Add some amazing feature'`)
1. Push to the branch (`git push origin feature/amazing-feature`)
1. Open a pull request

Please make sure to update tests as appropriate.

## ❤ Show your support

Give a ⭐️ if this project helped you!

<a href="https://www.buymeacoffee.com/leeturner" target="_blank">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="160">
</a>

## ☑️ TODO

- [ ] Setup releases using jReleaser

## 📝 License

```
Copyright © 2023 - Lee Turner

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

_This README was generated by [readgen](https://github.com/theapache64/readgen)_ ❤