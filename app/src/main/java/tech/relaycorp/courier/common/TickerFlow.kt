package tech.relaycorp.courier.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

fun tickerFlow(duration: Duration) = flow {
    while (true) {
        emit(Unit)
        delay(duration)
    }
}
