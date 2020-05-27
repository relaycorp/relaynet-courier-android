<img src="./courier-icon.png" align="right"/>

# Relaynet Courier for Android

This repository contains the source code for the [Relaynet Courier for Android](https://play.google.com/store/apps/details?id=tech.replaycorp.courier). 
As a Relaynet Courier implementation, its sole function is to relay cargo between Relaynet gateways. 
To learn more about Relaynet, visit [relaynet.network](https://relaynet.network).

This document is aimed at advanced users and (prospective) contributors. We aim to make the app as 
simple and intuitive as possible, and we're therefore not planning on publishing end-user 
documentation at this point.

## Multiple IP addresses in the private subnet are unsupported

If you've rooted your Android device or flashed it with a custom ROM, you might be able to have 
multiple IP address in the subnet `192.168.43.0/24`. If that were the case, the courier app will 
fail to allow incoming connections from private gateways: We need exactly one IP address in that 
range so that the app can self-issue TLS certificates for it.

## Architecture

The app follows clean architecture principals. Domain logic is separated from external elements
such as UI and data. The main components / layers / packages are:
 
 - `domain` - Domain logic, with one class per use-case
 - `ui` - Presentation logic, organized per screen, and following an [MVVM](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) pattern
 - `data` - Data persistence logic using preferences, database and disk
 - `background` - Background connect state listeners
 - `cogrpc` - Implementation of the [CogRPC](https://specs.relaynet.network/RS-008) protocol

Components are tied by dependency injection using [Dagger](https://dagger.dev). 
Kotlin coroutines and flow are used for threading and reactive design. 
For the views material components were preferred whenever possible.

## Development

The project should build and run out-of-the-box with Android Studio 4+. 
The minimum Android OS version supported is Android 5 (Lollipop, API 21).

### Run Android lint

```
./gradlew lint 
```

### Run kotlin lint

```
./gradlew spotlessCheck 
```

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

We love contributions! If you haven't contributed to a Relaycorp project before, please take a minute to [read our guidelines](https://github.com/relaycorp/.github/blob/master/CONTRIBUTING.md) first.

Please note that we're not accepting translations just yet as the copy is likely to change frequently and radically until the beta release. We're just trying to be mindful of your time and ours! Once the copy reaches a relatively stable state, we'll want to translate the app into as many languages as possible.
