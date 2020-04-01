package tech.relaycorp.courier.data.network.cogrpc

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import tech.relaycorp.courier.common.BehaviorChannel

class MockCogRPCServer(
    networkLocation: String
) : CogRPCServer(networkLocation) {

    private val clientsConnected = BehaviorChannel(0)

    override var isStarted: Boolean = false

    private var fakeClientThread: Job? = null

    override suspend fun start(
        connectionService: ConnectionService,
        onForcedStop: ((Throwable) -> Unit)
    ) {
        isStarted = true

        fakeClientThread =
            GlobalScope.launch {

                delay(1000)

                clientsConnected.send(1)

                val cargoes = connectionService.collectCargo(
                    CogRPC.MessageReceived(ByteArray(0).inputStream())
                )

                cargoes.forEach {
                    delay(500)
                    it.data.close()
                    connectionService.processCargoCollectionAck(CogRPC.MessageDeliveryAck(it.localId))
                }

                delay(3000)

                repeat(3) {
                    connectionService.deliverCargo(
                        CogRPC.MessageReceived(ByteArray(0).inputStream())
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
}
