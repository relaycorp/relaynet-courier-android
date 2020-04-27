package tech.relaycorp.relaynet.cogrpc.client

import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.relaynet.cogrpc.Authorization
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.CogRPC
import java.net.InetSocketAddress
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

class CogRPCClient(
    serverAddress: String,
    useTls: Boolean = true
) {

    private val channel by lazy {
        val url = URL(serverAddress)
        val address = InetSocketAddress(
            url.host,
            url.port.let { if (it != -1) it else DEFAULT_PORT }
        )
        NettyChannelBuilder
            .forAddress(address)
            .useTransportSecurity()
            .run { if (useTls) useTransportSecurity() else usePlaintext() }
            .build()
    }

    @Throws(Exception::class)
    fun deliverCargo(cargoes: Iterable<CogRPC.MessageDelivery>): Flow<CogRPC.MessageDeliveryAck> {
        if (cargoes.none()) return emptyFlow()

        val cargoesToAck = cargoes.map { it.localId }.toMutableList()
        val ackChannel = BroadcastChannel<CogRPC.MessageDeliveryAck>(1)
        val ackObserver = object : StreamObserver<CargoDeliveryAck> {
            override fun onNext(value: CargoDeliveryAck) {
                logger.info("deliverCargo ack ${value.id}")
                ackChannel.sendBlocking(CogRPC.MessageDeliveryAck(value))
                cargoesToAck.remove(value.id)
                if (cargoesToAck.isEmpty()) {
                    logger.info("deliverCargo ack complete")
                    ackChannel.close()
                }
            }

            override fun onError(t: Throwable) {
                logger.log(Level.WARNING, "deliverCargo ack error", t)
                ackChannel.close(t)
            }

            override fun onCompleted() {
                logger.info("deliverCargo ack closed")
                ackChannel.close()
            }
        }

        val client = buildClient()
        val deliveryObserver = client.deliverCargo(ackObserver)

        cargoes.forEach { delivery ->
            logger.info("deliverCargo next ${delivery.localId}")
            deliveryObserver.onNext(delivery.toCargoDelivery())
        }
        logger.info("deliverCargo complete")
        deliveryObserver.onCompleted()

        return ackChannel.asFlow()
    }

    @Throws(Exception::class)
    fun collectCargo(cca: CogRPC.MessageDelivery): Flow<CogRPC.MessageDelivery> {
        val ackChannel = BroadcastChannel<CogRPC.MessageDeliveryAck>(1)
        return channelFlow {
            val collectObserver = object : StreamObserver<CargoDelivery> {
                override fun onNext(value: CargoDelivery) {
                    logger.info("collectCargo ${value.id}")
                    this@channelFlow.sendBlocking(CogRPC.MessageDelivery(value))
                    ackChannel.sendBlocking(CogRPC.MessageDeliveryAck(value.id))
                }

                override fun onError(t: Throwable) {
                    logger.log(Level.WARNING, "collectCargo error", t)
                    this@channelFlow.close(t)
                }

                override fun onCompleted() {
                    logger.info("collectCargo complete")
                    this@channelFlow.close()
                    ackChannel.close()
                }
            }

            val client = buildAuthorizedClient(cca.data.readBytes())
            val ackObserver = client.collectCargo(collectObserver)
            ackChannel
                .asFlow()
                .onEach { ackObserver.onNext(it.toCargoDeliveryAck()) }
                .onCompletion { ackObserver.onCompleted() }
                .collect()
        }
    }

    private fun buildClient() = CargoRelayGrpc.newStub(channel)

    private fun buildAuthorizedClient(cca: ByteArray) =
        Authorization.authorizeClientWithCCA(buildClient(), cca)

    class Exception : Error()

    object Builder {
        fun build(serverAddress: String, useTls: Boolean = true) =
            CogRPCClient(serverAddress, useTls)
    }

    companion object {
        internal val logger = Logger.getLogger(CogRPCClient::class.java.name)
        private const val DEFAULT_PORT = 443
    }
}
