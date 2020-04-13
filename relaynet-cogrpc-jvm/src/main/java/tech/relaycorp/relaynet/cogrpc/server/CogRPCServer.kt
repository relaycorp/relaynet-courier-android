package tech.relaycorp.relaynet.cogrpc.server

import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import tech.relaycorp.relaynet.CargoRelayServer
import java.net.InetSocketAddress
import java.util.logging.Logger

class CogRPCServer
internal constructor(
    networkLocation: String
) : CargoRelayServer {

    override val isStarted get() = _isStarted
    private var _isStarted = false

    private lateinit var job: Job
    private val coroutineScope get() = CoroutineScope(Dispatchers.Main + job)

    private var server: Server? = null

    private val socketAddress = networkLocation.split(":").let {
        InetSocketAddress(it[0], it[1].toInt())
    }
    private val certificateInputStream get() = getResource("cert.pem")
    private val keyInputStream get() = getResource("key.pem")

    override suspend fun start(
        connectionService: CargoRelayServer.ConnectionService,
        onForcedStop: (Throwable) -> Unit
    ) {
        job = SupervisorJob()
        _isStarted = true

        withContext(Dispatchers.IO) {
            server = NettyServerBuilder
                .forAddress(socketAddress)
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .maxInboundMetadataSize(MAX_METADATA_SIZE)
                .useTransportSecurity(certificateInputStream, keyInputStream)
                .addService(CogRPCService(coroutineScope, connectionService))
                .intercept(Authorization.interceptor)
                .build()
                .start()
        }

        logger.info("Server started")
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            server?.shutdown()
            server = null
        }

        job.cancel()
        _isStarted = false

        logger.info("Server stopped")
    }

    override fun clientsConnected() = emptyFlow<Int>()

    private fun getResource(path: String) =
        javaClass.classLoader.getResourceAsStream(path)

    object Builder : CargoRelayServer.Builder {
        override fun build(networkLocation: String) = CogRPCServer(networkLocation)
    }

    companion object {
        private const val MAX_MESSAGE_SIZE = 10_000_000
        private const val MAX_METADATA_SIZE = 10_000 // CCAs
    }

    private val logger = Logger.getLogger(this.javaClass.name)
}
