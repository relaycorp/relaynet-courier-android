package tech.relaycorp.cogrpc.server

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.CogRPC
import java.util.logging.Level

class CogRPCConnectionService(
    private val coroutineScope: CoroutineScope,
    private val serverService: CogRPCServer.Service
) : CargoRelayGrpc.CargoRelayImplBase() {

    override fun deliverCargo(responseObserver: StreamObserver<CargoDeliveryAck>) =
        object : StreamObserver<CargoDelivery> {
            override fun onNext(cargoDelivery: CargoDelivery) {
                coroutineScope.launch {
                    logger.info("deliverCargo next ${cargoDelivery.id}")
                    val messageDelivery = CogRPC.MessageDelivery(cargoDelivery)
                    val result = serverService.deliverCargo(messageDelivery)
                    if (result) {
                        logger.info("deliverCargo next ack ${cargoDelivery.id}")
                        responseObserver.onNext(messageDelivery.toCargoDeliveryAck())
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                logger.log(Level.WARNING, "deliverCargo error", throwable)
            }

            override fun onCompleted() {
                logger.info("deliverCargo complete")
                coroutineScope.launch {
                    responseObserver.onCompleted()
                }
            }
        }

    override fun collectCargo(responseObserver: StreamObserver<CargoDelivery>): StreamObserver<CargoDeliveryAck> {
        val cca = AuthorizationContext.getCCA()
        if (cca == null) {
            logger.info("collectCargo completed due to missing CCA")
            responseObserver.onCompleted()
            return NoopStreamObserver()
        }

        val deliveriesToAck = mutableListOf<String>()

        coroutineScope.launch {
            try {
                val deliveries = getDeliveriesForCCA(cca)
                if (deliveries.any()) {
                    deliveriesToAck.addAll(deliveries.map { it.localId })
                    deliveries.forEach { messageDelivery ->
                        logger.info("collectCargo delivering ${messageDelivery.localId}")
                        responseObserver.onNext(messageDelivery.toCargoDelivery())
                    }
                } else {
                    logger.info("collectCargo completed empty")
                    responseObserver.onCompleted()
                }
            } catch (exception: Exception) {
                logger.log(Level.SEVERE, "collectCargo error", exception)
            }
        }

        return object : StreamObserver<CargoDeliveryAck> {
            override fun onNext(ack: CargoDeliveryAck) {
                logger.info("collectCargo ack next ${ack.id}")
                coroutineScope.launch {
                    try {
                        serverService.processCargoCollectionAck(CogRPC.MessageDeliveryAck(ack))
                        deliveriesToAck.remove(ack.id)
                        if (deliveriesToAck.isEmpty()) {
                            logger.info("collectCargo completed")
                            responseObserver.onCompleted()
                        }
                    } catch (exception: Exception) {
                        logger.log(Level.SEVERE, "collectCargo process ack error", exception)
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                logger.log(Level.WARNING, "collectCargo ack error", throwable)
            }

            override fun onCompleted() {
                logger.info("collectCargo ack closed")
            }
        }
    }

    private suspend fun getDeliveriesForCCA(cca: ByteArray): Iterable<CogRPC.MessageDelivery> {
        val messageReceived = CogRPC.MessageDelivery(data = cca.inputStream())
        return serverService.collectCargo(messageReceived)
    }
}
