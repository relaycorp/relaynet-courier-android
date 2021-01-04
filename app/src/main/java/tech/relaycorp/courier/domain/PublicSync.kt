package tech.relaycorp.courier.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.domain.client.CargoCollection
import tech.relaycorp.courier.domain.client.CargoDelivery
import tech.relaycorp.doh.DoHClient
import java.util.logging.Level
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
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Public sync error", e)
            state.send(State.Error)
        }
    }

    private suspend fun syncUnhandled() {
        val dohClient = DoHClient(DOH_RESOLVER_URL)
        dohClient.use {
            state.send(State.DeliveringCargo)
            cargoDelivery.deliver(dohClient)

            state.send(State.Waiting)
            delay(WAIT_PERIOD)

            state.send(State.CollectingCargo)
            cargoCollection.collect(dohClient)
        }

        state.send(State.Finished)
    }

    enum class State {
        DeliveringCargo, Waiting, CollectingCargo, Finished, Error
    }

    companion object {
        private val WAIT_PERIOD = 2.seconds

        // TODO: Remove this once we can use CloudFlare
        // https://github.com/cloudflare/cloudflare-docs/issues/565
        private const val DOH_RESOLVER_URL = "https://dns.google/dns-query"
    }
}
