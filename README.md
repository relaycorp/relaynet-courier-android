<img src="./courier-icon.png" style="float:right; margin: 0.5em; max-width: 40%"/>

# Relaynet Courier for Android

This repository contains the source code for the [Relaynet Courier for Android](https://play.google.com/store/apps/details?id=tech.replaycorp.courier). As a Relaynet Courier implementation, its sole function is to relay cargo between Relaynet gateways. To learn more about Relaynet, visit [relaynet.network](https://relaynet.network).

This document is aimed at advanced users and (prospective) contributors. We aim to make the app as simple and intuitive as possible, and we're therefore not planning on publishing end-user documentation at this point.

## Multiple IP address in the private subnet are unsupported

If you've rooted your Android device or flashed it with a custom aftermarket ROM, you might be able to have multiple IP address in the subnet `192.168.43.0/24`. If that were the case, the courier app will fail to allow incoming connections from private gateways: We need exactly one IP address in that range so that the app can self-issue TLS certificates for it.

## Architecture

TODO: Write anything that an experienced Android developer would like to know to understand the high-level design of the app. Here's an example for a server-side app: https://docs.relaycorp.tech/relaynet-pong/#architecture-and-backing-services

## Development

TODO: Add anything else that may be relevant

### Run unit tests

```
./gradlew testDebug 
```

### Run instrumentation tests

Requires an Android device or emulator

```
./gradlew connectedAndroidTest 
```

### Generate test code coverage

```
./gradlew jacocoAndroidTestReport 
```

## Contributing

We love contributions! If you're about to contribute to a Relaycorp project for the first time, please refer to [our guidelines](https://github.com/relaycorp/.github/blob/master/CONTRIBUTING.md) first.

Please note that we're not accepting translations just yet as the copy is likely to change frequently and radically until the beta release. We're just trying to be mindful of your time and ours! Once the copy reaches a relatively stable state, we'll want to translate the app into as many languages as possible.
