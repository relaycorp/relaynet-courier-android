package tech.relaycorp.cogrpc.server

import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.conscrypt.Conscrypt
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.relaynet.cogrpc.CogRPC
import java.net.InetSocketAddress
import java.security.Security

class CogRPCServer
internal constructor(
    private val hostname: String,
    private val port: Int
) {

    var isStarted = false
        private set

    private val job = SupervisorJob()
    private val coroutineScope get() = CoroutineScope(Dispatchers.Main + job)

    private var server: Server? = null

    private val clientsInterceptor by lazy { ClientsConnectedFilter() }
    private val certificateInputStream get() = getResource("cert.pem")
    private val keyInputStream get() = getResource("key.pem")

    suspend fun start(
        service: Service,
        onForcedStop: (Throwable) -> Unit = {}
    ) {
        isStarted = true

        withContext(Dispatchers.IO) {
            setupTLSProvider()

            server = NettyServerBuilder
                .forAddress(InetSocketAddress(hostname, port))
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .maxInboundMetadataSize(MAX_METADATA_SIZE)
                .useTransportSecurity(certificateInputStream, keyInputStream)
                .addService(CogRPCConnectionService(coroutineScope, service))
                .intercept(Authorization.interceptor)
                .addTransportFilter(clientsInterceptor)
                .build()
                .start()
        }

        logger.info("Server started")
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            server?.shutdown()
            server = null
        }

        job.cancel()
        isStarted = false

        logger.info("Server stopped")
    }

    fun clientsConnected() = clientsInterceptor.clientsCount()

    private fun getResource(path: String) =
        javaClass.classLoader!!.getResourceAsStream(path)

    private fun setupTLSProvider() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    object Builder {
        fun build(hostname: String, port: Int) = CogRPCServer(hostname, port)
    }

    companion object {
        private const val MAX_MESSAGE_SIZE = 8_397_056
        private const val MAX_METADATA_SIZE = 2_048 // CCAs
    }

    interface Service {
        suspend fun collectCargo(cca: CogRPC.MessageReceived): Iterable<CogRPC.MessageDelivery>
        suspend fun processCargoCollectionAck(ack: CogRPC.MessageDeliveryAck)
        suspend fun deliverCargo(cargo: CogRPC.MessageReceived)
    }
}
