package tech.relaycorp.relaynet.cogrpc.server

import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tech.relaycorp.relaynet.CargoRelay
import tech.relaycorp.relaynet.CargoRelayServer
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import java.util.logging.Level
import java.util.logging.Logger

class CogRPCService(
    private val coroutineScope: CoroutineScope,
    private val connectionService: CargoRelayServer.ConnectionService
) : CargoRelayGrpc.CargoRelayImplBase() {

    override fun deliverCargo(responseObserver: StreamObserver<CargoDeliveryAck>) =
        object : StreamObserver<CargoDelivery> {
            override fun onNext(cargoDelivery: CargoDelivery) {
                coroutineScope.launch {
                    logger.info("deliverCargo next")
                    connectionService.deliverCargo(cargoDelivery.toMessageReceived())
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
        val cca = Authorization.getCCA()
        coroutineScope.launch {
            try {
                val messageReceived = CargoRelay.MessageReceived(cca.inputStream())
                val deliveries = connectionService.collectCargo(messageReceived)
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

        return object : StreamObserver<CargoDeliveryAck> {
            override fun onNext(ack: CargoDeliveryAck) {
                logger.info("collectCargo ack next")
                coroutineScope.launch {
                    connectionService.processCargoCollectionAck(ack.toMessageDeliveryAck())
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

    internal fun CargoDelivery.toMessageReceived() =
        CargoRelay.MessageReceived(data = cargo.newInput())

    private fun CargoRelay.MessageDelivery.toCargoDelivery() =
        CargoDelivery.newBuilder()
            .setId(localId)
            .setCargo(ByteString.copyFrom(data.readBytes()))
            .build()

    internal fun CargoDelivery.toAck() =
        CargoDeliveryAck.newBuilder()
            .setId(id)
            .build()

    internal fun CargoDeliveryAck.toMessageDeliveryAck() =
        CargoRelay.MessageDeliveryAck(id)

    internal val logger = Logger.getLogger(javaClass.name)
}
