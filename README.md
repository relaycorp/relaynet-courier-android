# Relaynet Courier Android app

[Relaynet](https://relaynet.link) Courier Android app.

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

## TODO

### Remove Netty Conscrypt fix

Once this Netty commit (https://github.com/netty/netty/commit/79ef0c4706b64bd0b6c3ce24516beb587a0c5f4a) 
gets released, we can remove the overload class `io.netty.handler.ssl.Conscrypt` in this module,
and its netty-handler dependency on `build.gradle`. 
