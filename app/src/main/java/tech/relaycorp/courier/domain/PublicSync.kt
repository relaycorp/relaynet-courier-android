package tech.relaycorp.courier.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import tech.relaycorp.courier.common.BehaviorChannel
import javax.inject.Inject
import kotlin.time.seconds

class PublicSync
@Inject constructor(
    private val deliverPublicCargo: DeliverPublicCargo,
    private val collectPublicCargo: CollectPublicCargo
) {

    private val state = BehaviorChannel<State>()
    fun state() = state.asFlow()

    private val dataTransferred = BehaviorChannel<DataCount>()
    fun dataTransferred() = dataTransferred.asFlow()

    suspend fun sync() {
        // TODO: Collect up-to-date data transferred data
        dataTransferred.send(DataCount())

        state.send(State.DeliveringCargo)
        deliverPublicCargo.deliver()

        state.send(State.Waiting)
        delay(WAIT_PERIOD)

        state.send(State.CollectingCargo)
        collectPublicCargo.collect()

        state.send(State.Finished)
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
