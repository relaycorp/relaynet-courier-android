package tech.relaycorp.relaynet.cogrpc

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import tech.relaycorp.relaynet.CargoRelay
import tech.relaycorp.relaynet.CargoRelayServer

class MockCargoRelayServer
internal constructor(
    networkLocation: String
) : CargoRelayServer {

    private val clientsConnected = ConflatedBroadcastChannel(0)

    override var isStarted: Boolean = false

    private var fakeClientThread: Job? = null

    override suspend fun start(
        connectionService: CargoRelayServer.ConnectionService,
        onForcedStop: ((Throwable) -> Unit)
    ) {
        isStarted = true

        fakeClientThread =
            GlobalScope.launch {

                delay(1000)

                clientsConnected.send(1)

                val cargoes = connectionService.collectCargo(
                    CargoRelay.MessageReceived("CARGO".toByteArray().inputStream())
                )

                cargoes.forEach {
                    delay(500)
                    it.data.close()
                    connectionService.processCargoCollectionAck(CargoRelay.MessageDeliveryAck(it.localId))
                }

                delay(3000)

                repeat(3) {
                    connectionService.deliverCargo(
                        CargoRelay.MessageReceived("CARGO".toByteArray().inputStream())
                    )
                }
                clientsConnected.send(0)
            }
    }

    override suspend fun stop() {
        isStarted = false
        fakeClientThread?.cancel()
    }

    override fun clientsConnected() = clientsConnected.asFlow()

    object Builder : CargoRelayServer.Builder {
        override fun build(networkLocation: String) =
            MockCargoRelayServer(networkLocation)
    }
}
