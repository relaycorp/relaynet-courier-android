# Relaynet Courier Android app

[Relaynet](https://relaynet.link) Courier Android app.

## Limitations

- Rooted devices or those using a fork of Android must have exactly one IP address in the subnet `192.168.43.0/24`, or else the courier app will fail to allow incoming connections from private gateways: We need exactly one IP address in that range so that the app can self-issue TLS certificates for it. This shouldn't be a problem with an unmodified OS.

## Development

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
