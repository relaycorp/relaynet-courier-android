package tech.relaycorp.cogrpc.server

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.toAck
import tech.relaycorp.relaynet.cogrpc.toCargoDelivery
import java.util.logging.Level

class CogRPCConnectionService(
    private val coroutineScope: CoroutineScope,
    private val serverService: CogRPCServer.Service
) : CargoRelayGrpc.CargoRelayImplBase() {

    override fun deliverCargo(responseObserver: StreamObserver<CargoDeliveryAck>) =
        object : StreamObserver<CargoDelivery> {
            private var isResponseFinished = false

            override fun onNext(cargoDelivery: CargoDelivery) {
                coroutineScope.launch {
                    logger.info("deliverCargo next ${cargoDelivery.id}")
                    val cargoInputStream = cargoDelivery.cargo.newInput()
                    val result = serverService.deliverCargo(cargoInputStream)
                    if (isResponseFinished) return@launch
                    when (result) {
                        CogRPCServer.DeliverResult.Successful -> {
                            logger.info("deliverCargo next ack ${cargoDelivery.id}")
                            responseObserver.onNext(cargoDelivery.toAck())
                        }
                        CogRPCServer.DeliverResult.UnavailableStorage -> {
                            logger.info("deliverCargo no space available for ${cargoDelivery.id}")
                            logger.info("deliverCargo closing with error")
                            responseObserver.onError(StatusRuntimeException(Status.RESOURCE_EXHAUSTED))
                            isResponseFinished = true
                        }
                        CogRPCServer.DeliverResult.Invalid,
                        CogRPCServer.DeliverResult.Malformed -> {
                            logger.info("deliverCargo invalid or malformed cargo ${cargoDelivery.id}")
                            logger.info("deliverCargo closing with error")
                            responseObserver.onError(StatusRuntimeException(Status.INVALID_ARGUMENT))
                            isResponseFinished = true
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                logger.log(Level.WARNING, "deliverCargo error", throwable)
            }

            override fun onCompleted() {
                logger.info("deliverCargo complete")
                coroutineScope.launch {
                    isResponseFinished = true
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
                        serverService.processCargoCollectionAck(ack.id)
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

    private suspend fun getDeliveriesForCCA(cca: ByteArray): Iterable<CargoDeliveryRequest> =
        serverService.collectCargo(cca)
}
