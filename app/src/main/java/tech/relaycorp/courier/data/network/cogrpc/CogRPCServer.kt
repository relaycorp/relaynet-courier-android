package tech.relaycorp.courier.data.network.cogrpc

abstract class CogRPCServer
protected constructor(
    val networkLocation: String
) {

    abstract var isStarted: Boolean
        protected set

    abstract suspend fun start(
        connectionService: ConnectionService,
        onForcedStop: ((Throwable) -> Unit)
    )

    abstract suspend fun stop()

    interface ConnectionService {
        suspend fun collectCargo(cca: CogRPC.MessageReceived): Iterable<CogRPC.MessageDelivery>
        suspend fun processCargoCollectionAck(ack: CogRPC.MessageDeliveryAck)
        suspend fun deliverCargo(cargo: CogRPC.MessageReceived)
    }

    companion object {
        fun build(networkLocation: String): CogRPCServer = MockCogRPCServer(networkLocation)
    }
}
