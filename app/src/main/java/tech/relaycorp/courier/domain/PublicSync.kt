package tech.relaycorp.courier.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.domain.client.CargoCollection
import tech.relaycorp.courier.domain.client.CargoDelivery
import tech.relaycorp.courier.domain.client.InternetAddressResolver
import tech.relaycorp.doh.DoHClient
import java.util.logging.Level
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class PublicSync
    @Inject
    constructor(
        private val cargoDelivery: CargoDelivery,
        private val cargoCollection: CargoCollection,
    ) {
        private val state = MutableStateFlow<State?>(null)

        fun state() = state.asStateFlow().filterNotNull()

        suspend fun sync() {
            try {
                syncUnhandled()
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Public sync error", e)
                state.value = State.Error
            }
        }

        private suspend fun syncUnhandled() {
            DoHClient(DOH_RESOLVER_URL).use {
                val resolver = InternetAddressResolver(it)

                state.value = State.DeliveringCargo
                cargoDelivery.deliver(resolver)

                state.value = State.Waiting
                delay(WAIT_PERIOD)

                state.value = State.CollectingCargo
                cargoCollection.collect(resolver)
            }

            state.value = State.Finished
        }

        enum class State {
            DeliveringCargo,
            Waiting,
            CollectingCargo,
            Finished,
            Error,
        }

        companion object {
            private val WAIT_PERIOD = 10.seconds

            // TODO: Remove this once we can use CloudFlare
            // https://github.com/cloudflare/cloudflare-docs/issues/565
            private const val DOH_RESOLVER_URL = "https://dns.google/dns-query"
        }
    }
