package tech.relaycorp.relaynet.cogrpc.client

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
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
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.AuthorizationMetadata
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.toCargoDelivery
import tech.relaycorp.relaynet.cogrpc.toCargoDeliveryAck
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.seconds

class CogRPCClient
private constructor(
    serverAddress: String,
    val useTls: Boolean = true
) {

    internal val address by lazy {
        val url = URL(serverAddress)
        val fallbackPort = if (url.protocol == "https") 443 else 80
        InetSocketAddress(
            url.host,
            url.port.let { if (it != -1) it else fallbackPort }
        )
    }

    internal val channel by lazy {
        NettyChannelBuilder
            .forAddress(address)
            .run { if (useTls) useTransportSecurity() else usePlaintext() }
            .build()
    }

    fun deliverCargo(cargoes: Iterable<CargoDeliveryRequest>): Flow<String> {
        if (cargoes.none()) return emptyFlow()

        val cargoesToAck = mutableListOf<String>()
        val ackChannel = BroadcastChannel<String>(1)
        val ackObserver = object : StreamObserver<CargoDeliveryAck> {
            override fun onNext(value: CargoDeliveryAck) {
                logger.info("deliverCargo ack ${value.id}")
                ackChannel.sendBlocking(value.id)
                cargoesToAck.remove(value.id)
            }

            override fun onError(t: Throwable) {
                logger.log(Level.WARNING, "deliverCargo ack error", t)
                ackChannel.close(CogRPCException(t))
            }

            override fun onCompleted() {
                logger.info("deliverCargo ack closed")
                ackChannel.close()
                if (cargoesToAck.any()) {
                    logger.info("deliverCargo server did not acknowledge all cargo deliveries")
                }
            }
        }

        val client = buildClient()
        val deliveryObserver = client.deliverCargo(ackObserver)

        cargoes.forEach { delivery ->
            logger.info("deliverCargo next ${delivery.localId}")
            cargoesToAck.add(delivery.localId)
            deliveryObserver.onNext(delivery.toCargoDelivery())
        }
        logger.info("deliverCargo complete")
        deliveryObserver.onCompleted()

        return ackChannel.asFlow()
    }

    fun collectCargo(cca: InputStream): Flow<InputStream> {
        val ackChannel = BroadcastChannel<String>(1)
        return channelFlow {
            val collectObserver = object : StreamObserver<CargoDelivery> {
                override fun onNext(value: CargoDelivery) {
                    logger.info("collectCargo ${value.id}")
                    this@channelFlow.sendBlocking(value.cargo.newInput())
                    ackChannel.sendBlocking(value.id)
                }

                override fun onError(t: Throwable) {
                    logger.log(Level.WARNING, "collectCargo error", t)
                    this@channelFlow.close(
                        if (t is StatusException && t.status == Status.PERMISSION_DENIED) {
                            CCARefusedException()
                        } else {
                            CogRPCException(t)
                        }
                    )
                }

                override fun onCompleted() {
                    logger.info("collectCargo complete")
                    this@channelFlow.close()
                    ackChannel.close()
                }
            }

            val client = buildAuthorizedClient(cca.readBytes())
            val ackObserver = client.collectCargo(collectObserver)
            ackChannel
                .asFlow()
                .onEach { ackObserver.onNext(it.toCargoDeliveryAck()) }
                .onCompletion { ackObserver.onCompleted() }
                .collect()
        }
    }

    fun close() {
        channel.shutdown()
    }

    private fun buildClient() =
        CargoRelayGrpc.newStub(channel)
            .withDeadlineAfter(CALL_DEADLINE.inSeconds.toLong(), TimeUnit.SECONDS)

    private fun buildAuthorizedClient(cca: ByteArray) =
        MetadataUtils.attachHeaders(
            buildClient(),
            AuthorizationMetadata.makeMetadata(cca)
        )

    open class CogRPCException(throwable: Throwable? = null) : Exception(throwable)
    class CCARefusedException : CogRPCException()

    object Builder {
        fun build(serverAddress: String, useTls: Boolean = true) =
            CogRPCClient(serverAddress, useTls)
    }

    companion object {
        internal val logger = Logger.getLogger(CogRPCClient::class.java.name)
        internal val CALL_DEADLINE = 5.seconds
    }
}
