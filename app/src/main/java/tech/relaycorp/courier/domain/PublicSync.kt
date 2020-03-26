package tech.relaycorp.courier.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.data.network.cogrpc.CogRPCClient
import tech.relaycorp.courier.domain.client.CargoCollection
import tech.relaycorp.courier.domain.client.CargoDelivery
import javax.inject.Inject
import kotlin.time.seconds

class PublicSync
@Inject constructor(
    private val cargoDelivery: CargoDelivery,
    private val cargoCollection: CargoCollection
) {

    private val state = BehaviorChannel<State>()
    fun state() = state.asFlow()

    suspend fun sync() {
        try {
            syncUnhandled()
        } catch (e: CogRPCClient.Exception) {
            state.send(State.Error)
        }
    }

    private suspend fun syncUnhandled() {
        state.send(State.DeliveringCargo)
        cargoDelivery.deliver()

        state.send(State.Waiting)
        delay(WAIT_PERIOD)

        state.send(State.CollectingCargo)
        cargoCollection.collect()

        state.send(State.Finished)
    }

    enum class State {
        DeliveringCargo, Waiting, CollectingCargo, Finished, Error
    }

    companion object {
        private val WAIT_PERIOD = 2.seconds
    }
}
