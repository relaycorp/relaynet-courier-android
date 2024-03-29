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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CogRPCServer
    internal constructor(
        private val hostname: String,
        private val port: Int,
    ) {
        var isStarted = false
            private set

        private val job = SupervisorJob()
        private val coroutineScope get() = CoroutineScope(Dispatchers.IO + job)

        private var server: Server? = null

        private val clientsInterceptor by lazy { ClientsConnectedFilter() }

        internal var tlsCertificateGeneratorProvider: ((ipAddress: String) -> TLSCertificateGenerator) =
            TLSCertificateGenerator.Companion::generate

        suspend fun start(
            service: Service,
            onForcedStop: (Throwable) -> Unit = {},
        ) {
            isStarted = true

            withContext(Dispatchers.IO) {
                setupTLSProvider()

                val certGenerator = tlsCertificateGeneratorProvider(Networking.getGatewayIpAddress())

                val server =
                    NettyServerBuilder
                        .forAddress(InetSocketAddress(hostname, port))
                        .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                        .maxInboundMetadataSize(MAX_METADATA_SIZE)
                        .maxConcurrentCallsPerConnection(MAX_CONCURRENT_CALLS_PER_CONNECTION)
                        .maxConnectionAge(MAX_CONNECTION_AGE.inWholeSeconds, TimeUnit.SECONDS)
                        .maxConnectionAgeGrace(MAX_CONNECTION_AGE_GRACE.inWholeSeconds, TimeUnit.SECONDS)
                        .maxConnectionIdle(MAX_CONNECTION_IDLE.inWholeSeconds, TimeUnit.SECONDS)
                        .useTransportSecurity(
                            certGenerator.exportCertificate().inputStream(),
                            certGenerator.exportPrivateKey().inputStream(),
                        )
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
            server
                ?.shutdown()
                ?.awaitTermination(TERMINATION_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
            server = null

            job.cancel()
            isStarted = false

            logger.info("Server stopped")
        }

        fun clientsConnected() = clientsInterceptor.clientsCount()

        private fun setupTLSProvider() {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        object Builder {
            fun build(
                hostname: String,
                port: Int,
            ) = CogRPCServer(hostname, port)
        }

        companion object {
            private const val MAX_MESSAGE_SIZE = 8_397_056
            private const val MAX_METADATA_SIZE = 6_000 // ~4.9kib for a base64-encoded CCA + overhead
            private const val MAX_CONCURRENT_CALLS_PER_CONNECTION = 3
            private val MAX_CONNECTION_AGE = 15.minutes
            private val MAX_CONNECTION_AGE_GRACE = 30.seconds
            private val MAX_CONNECTION_IDLE = 10.seconds
            private val TERMINATION_TIMEOUT = 5.seconds
        }

        interface Service {
            suspend fun collectCargo(ccaSerialized: ByteArray): Iterable<CargoDeliveryRequest>

            suspend fun processCargoCollectionAck(localId: String)

            suspend fun deliverCargo(cargoSerialized: InputStream): DeliverResult
        }

        enum class DeliverResult {
            Successful,
            Malformed,
            Invalid,
            UnavailableStorage,
        }
    }
