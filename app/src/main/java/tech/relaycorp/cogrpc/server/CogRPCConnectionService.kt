package tech.relaycorp.cogrpc.server

import com.google.protobuf.ByteString
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
                    logger.info("deliverCargo next")
                    serverService.deliverCargo(cargoDelivery.toMessageReceived())
                    logger.info("deliverCargo next ack")
                    responseObserver.onNext(cargoDelivery.toAck())
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
        Authorization.getCCA()?.let { cca ->
            collectCargoDelivery(responseObserver, cca)
        }

        return object : StreamObserver<CargoDeliveryAck> {
            override fun onNext(ack: CargoDeliveryAck) {
                logger.info("collectCargo ack next")
                coroutineScope.launch {
                    serverService.processCargoCollectionAck(ack.toMessageDeliveryAck())
                }
            }

            override fun onError(throwable: Throwable) {
                logger.log(Level.WARNING, "collectCargo ack error", throwable)
            }

            override fun onCompleted() {
                logger.info("collectCargo ack complete")
            }
        }
    }

    private fun collectCargoDelivery(
        responseObserver: StreamObserver<CargoDelivery>,
        cca: ByteArray
    ) {
        coroutineScope.launch {
            try {
                val messageReceived = CogRPC.MessageReceived(cca.inputStream())
                val deliveries = serverService.collectCargo(messageReceived)
                deliveries.forEach { messageDelivery ->
                    logger.info("collectCargo delivering ${messageDelivery.localId}")
                    responseObserver.onNext(messageDelivery.toCargoDelivery())
                }
            } catch (exception: Exception) {
                logger.log(Level.SEVERE, "collectCargo error", exception)
            }
            logger.info("collectCargo completed")
            responseObserver.onCompleted()
        }
    }

    internal fun CargoDelivery.toMessageReceived() =
        CogRPC.MessageReceived(data = cargo.newInput())

    private fun CogRPC.MessageDelivery.toCargoDelivery() =
        CargoDelivery.newBuilder()
            .setId(localId)
            .setCargo(ByteString.copyFrom(data.readBytes()))
            .build()

    internal fun CargoDelivery.toAck(): CargoDeliveryAck =
        CargoDeliveryAck.newBuilder()
            .setId(id)
            .build()

    internal fun CargoDeliveryAck.toMessageDeliveryAck() =
        CogRPC.MessageDeliveryAck(id)
}
