package tech.relaycorp.relaynet.cogrpc.client

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
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
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.cogrpc.toCargoDelivery
import tech.relaycorp.relaynet.cogrpc.toCargoDeliveryAck
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.seconds

open class CogRPCClient
internal constructor(
    serverAddress: String,
    val requireTls: Boolean = true
) {
    private val serverUrl = URL(serverAddress)

    init {
        if (requireTls && serverUrl.protocol != "https") {
            throw CogRPCException(message = "Cannot connect to $serverAddress with TLS required")
        }
    }

    private val address by lazy {
        val fallbackPort = if (serverUrl.protocol == "https") 443 else 80
        InetSocketAddress(
            serverUrl.host,
            serverUrl.port.let { if (it != -1) it else fallbackPort }
        )
    }

    internal val channel by lazy {
        val useTls = requireTls || serverUrl.protocol == "https"
        val isHostPrivateAddress = InetAddress.getByName(serverUrl.host).isSiteLocalAddress
        initialChannelBuilder
            .run { if (useTls) useTransportSecurity() else usePlaintext() }
            .let { if (useTls && isHostPrivateAddress) it.sslContext(insecureTlsContext) else it }
            .build()
    }

    internal open val initialChannelBuilder by lazy { NettyChannelBuilder.forAddress(address) }

    internal val insecureTlsContext by lazy {
        GrpcSslContexts.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
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

    fun collectCargo(cca: (() -> InputStream)): Flow<InputStream> {
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

            val client = buildAuthorizedClient(cca().readBytesAndClose())
            val ackObserver = client.collectCargo(collectObserver)
            ackChannel
                .asFlow()
                .onEach { ackObserver.onNext(it.toCargoDeliveryAck()) }
                .onCompletion { ackObserver.onCompleted() }
                .collect()
        }
    }

    fun close() {
        logger.info("Closing CogRPCClient")
        channel.shutdown().awaitTermination(CALL_DEADLINE.inSeconds.toLong(), TimeUnit.SECONDS)
    }

    private fun buildClient() =
        CargoRelayGrpc.newStub(channel)
            .withDeadlineAfter(CALL_DEADLINE.inSeconds.toLong(), TimeUnit.SECONDS)

    private fun buildAuthorizedClient(cca: ByteArray) =
        MetadataUtils.attachHeaders(
            buildClient(),
            AuthorizationMetadata.makeMetadata(cca)
        )

    open class CogRPCException(throwable: Throwable? = null, message: String? = null) :
        Exception(message, throwable)

    class CCARefusedException : CogRPCException()

    object Builder {
        fun build(serverAddress: String, requireTls: Boolean = true) =
            CogRPCClient(serverAddress, requireTls)
    }

    companion object {
        internal val logger = Logger.getLogger(CogRPCClient::class.java.name)
        internal val CALL_DEADLINE = 5.seconds
    }
}
