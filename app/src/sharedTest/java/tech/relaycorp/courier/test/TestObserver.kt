package tech.relaycorp.courier.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <T> Flow<T>.test(scope: CoroutineScope): TestObserver<T> {
    return TestObserver(scope, this)
}

class TestObserver<T>(
    scope: CoroutineScope,
    flow: Flow<T>,
) {
    private val values = mutableListOf<T>()
    private val job: Job =
        scope.launch {
            flow.collect { values.add(it) }
        }

    fun assertNoValues(): TestObserver<T> {
        assert(emptyList<T>() == this.values) { "Values is not empty: $values" }
        return this
    }

    fun assertValues(vararg values: T): TestObserver<T> {
        assert(values.toList() == this.values) { "Expected ${values.toList()} but got ${this.values}" }
        return this
    }

    fun finish() {
        job.cancel()
    }
}
