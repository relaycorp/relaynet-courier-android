package tech.relaycorp.courier.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import tech.relaycorp.courier.common.BehaviorChannel
import javax.inject.Inject
import kotlin.time.seconds

class PublicSync
@Inject constructor() {

    private val state = BehaviorChannel<State>()
    fun state() = state.asFlow()

    private val dataTransferred = BehaviorChannel<DataCount>()
    fun dataTransferred() = dataTransferred.asFlow()

    suspend fun sync() {
        dataTransferred.send(DataCount())
        state.send(State.DeliveringCargo)
        deliverCargo()
        state.send(State.Waiting)
        delay(WAIT_PERIOD)
        state.send(State.CollectingCargo)
        collectCargo()
        state.send(State.Finished)
    }

    private suspend fun deliverCargo() {
        delay(3.seconds)
    }

    private suspend fun collectCargo() {
        delay(3.seconds)
    }

    enum class State {
        DeliveringCargo, Waiting, CollectingCargo, Finished
    }

    data class DataCount(
        val upload: Long = 0,
        val download: Long = 0
    )

    companion object {
        private val WAIT_PERIOD = 2.seconds
    }
}
