# callback-logger

![buildStatus](https://img.shields.io/github/workflow/status/leeturner/callback-logger/Java%20CI%20with%20Gradle?style=plastic)
![latestVersion](https://img.shields.io/github/v/release/leeturner/callback-logger)
<a href="https://twitter.com/leeturner" target="_blank">
<img alt="Twitter: leeturner" src="https://img.shields.io/twitter/follow/leeturner.svg?style=social" />
</a>

> Simple service to receive callbacks. Useful for when testing APIs locally that send callbacks

## 🦿 Prerequisites

- Java 17 or above

## 🛠 Installation

1. Download latest sources and run:
 
 ```shell script
./gradlew build run
```

## ⌨️ Usage

Once up and running you can set the callback url of the API you are testing to the callback endpoint of `callback-logger` - 
`http://localhost:7070/callback-logger/api/callback`.  Both `POST` and `PUT` methods are supported for the callback 
endpoint along with multiple content types.  You can send a callback manually using the following `curl` request:

```shell
curl -X POST --location "http://localhost:7070/callback-logger/api/callback" \
    -H "Content-Type: application/json" \
    -d "{\"hello\": \"world\"}"
```

or

```shell
curl -X PUT --location "http://localhost:7070/callback-logger/api/callback" \
    -H "Content-Type: application/xml" \
    -d "<hello>world</hello>" 
```

### Custom callbacks

If you need to send callbacks to different callback endpoints than the default `callback-logger` endpoint then you can
define custom callbacks.  Custom callbacks are defined in the `application.conf` file and are defined as a list of `put`
or `post` URIs.  For example:

```hocon
callback-logger {
  custom-callbacks {
    post = ["/api/custom1", "/api/custom2"]
    put = ["/api/custom1"]
  }
}
```

Any duplicate URIs will be ignored and all custom URIs need to start with a `/`.  You can then send callbacks to the 
custom endpoints using the following `curl` request:

```shell
curl -X PUT --location "http://localhost:7070/api/custom1" \
    -H "Content-Type: application/xml" \
    -d "<hello>world</hello>" 
```
### Specify the http response code

By default, callback-logger returns a `200` response code for all callbacks.  If you need to return a different response
code then you can define the response code in the `application.conf` file.  For example:

```hocon
callback-logger {
  custom-callback-response-code = 201
  callback-response-payload = ""
  custom-callbacks {
    post = ["/api/custom1", "/api/custom2"]
    put = ["/api/custom1"]
  }
}
```

The application will fail to start if the response code is not a valid http response code.

### Specify the http response payload

By default, callback-logger returns an empty string as the response payload for all callbacks.  If you need to return a different response
payload then you can change that in the `application.conf` file.  For example:

```hocon
callback-logger {
  custom-callback-response-code = 200
  callback-response-payload = "{\"status\": \"OK\", \"message\": \"Callback received\"}"
  custom-callbacks {
    post = ["/api/custom1", "/api/custom2"]
    put = ["/api/custom1"]
  }
}
```

### Web UI

`callback-logger` provides a simple web interface to allow you to see the callbacks you have received.  You can access
the web interface by pointing your browser to `http://localhost:7070/callback-logger`:


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