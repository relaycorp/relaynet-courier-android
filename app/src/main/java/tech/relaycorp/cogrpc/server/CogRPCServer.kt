package tech.relaycorp.cogrpc.server

import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.conscrypt.Conscrypt
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.relaynet.CargoDeliveryRequest
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.security.Security
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlin.math.roundToLong
import kotlin.time.minutes
import kotlin.time.seconds

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

            val server = NettyServerBuilder
                .forAddress(InetSocketAddress(hostname, port))
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .maxInboundMetadataSize(MAX_METADATA_SIZE)
                .maxConcurrentCallsPerConnection(MAX_CONCURRENT_CALLS_PER_CONNECTION)
                .maxConnectionAge(MAX_CONNECTION_AGE.inSeconds.roundToLong(), TimeUnit.SECONDS)
                .maxConnectionIdle(MAX_CONNECTION_IDLE.inSeconds.roundToLong(), TimeUnit.SECONDS)
                .useTransportSecurity(certificateInputStream, keyInputStream)
                .addService(CogRPCConnectionService(coroutineScope, service))
                .intercept(AuthorizationContext.interceptor)
                .addTransportFilter(clientsInterceptor)
                .build()

            try {
                server.start()
                this@CogRPCServer.server = server
                logger.info("Server started")
            } catch (exception: IOException) {
                logger.log(Level.WARNING, "Could not start server", exception)
                onForcedStop.invoke(exception)
            }
        }
    }

    fun stop() {
        // server?.shutdown()
        server = null

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
        private const val MAX_CONCURRENT_CALLS_PER_CONNECTION = 3
        private val MAX_CONNECTION_AGE = 15.minutes
        private val MAX_CONNECTION_IDLE = 10.seconds
    }

    interface Service {
        suspend fun collectCargo(ccaSerialized: ByteArray): Iterable<CargoDeliveryRequest>
        suspend fun processCargoCollectionAck(localId: String)
        suspend fun deliverCargo(cargoSerialized: InputStream): Boolean
    }
}
