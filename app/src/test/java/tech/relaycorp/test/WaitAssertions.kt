package tech.relaycorp.test

import kotlinx.coroutines.delay

object WaitAssertions {

    suspend fun waitForAssertEquals(expected: Any, actualCheck: suspend () -> Any) {
        val initialTime = System.currentTimeMillis()
        var value = actualCheck.invoke()

        while (expected != value) {
            delay(TIMEOUT / 20)
            if (System.currentTimeMillis() - initialTime > TIMEOUT) {
                throw AssertionError("Timeout waiting for $value to become $expected")
            }
            value = actualCheck.invoke()
        }
    }

    suspend fun waitForAssertTrue(actualCheck: suspend () -> Boolean) {
        waitForAssertEquals(true, actualCheck)
    }

    private const val TIMEOUT = 10_000L
}
