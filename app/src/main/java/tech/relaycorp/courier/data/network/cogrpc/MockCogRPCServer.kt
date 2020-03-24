package tech.relaycorp.courier.data.network.cogrpc

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class MockCogRPCServer(
    networkLocation: String
) : CogRPCServer(networkLocation) {

    override var isStarted: Boolean = false

    override suspend fun start(
        connectionService: ConnectionService,
        onForcedStop: ((Throwable) -> Unit)
    ) {
        isStarted = true

        Thread {
            runBlocking {
                delay(1000)

                val cargoes = connectionService.collectCargo(
                    CogRPC.MessageReceived(ByteArray(0).inputStream())
                )

                cargoes.forEach {
                    delay(500)
                    connectionService.processCargoCollectionAck(CogRPC.MessageDeliveryAck(it.localId))
                }

                delay(3000)

                repeat(3) {
                    connectionService.deliverCargo(
                        CogRPC.MessageReceived(ByteArray(0).inputStream())
                    )
                }
            }
        }.start()
    }

    override suspend fun stop() {
        isStarted = false
    }
}
