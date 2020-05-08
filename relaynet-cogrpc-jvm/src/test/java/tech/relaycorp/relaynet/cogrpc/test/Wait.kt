package tech.relaycorp.relaynet.cogrpc.test

import kotlin.time.milliseconds
import kotlin.time.seconds

object Wait {

    fun <T> waitForNotNull(condition: (() -> T?)): T {
        var value = condition.invoke()
        val startTime = currentTimeDuration()
        while (value == null) {
            try {
                Thread.sleep(CHECK_INTERVAL.toLongMilliseconds())
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            if (currentTimeDuration() - startTime > TIMEOUT) {
                throw AssertionError("Wait timeout.")
            }
            value = condition.invoke()
        }
        return value
    }

    private fun currentTimeDuration() = System.currentTimeMillis().milliseconds

    private val CHECK_INTERVAL = 100.milliseconds
    private val TIMEOUT = 10.seconds
}
