package tech.relaycorp.cogrpc.server

import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.test.TestCoroutineScope
import java.util.UUID

class TestCogRPCServer(
    private val cogRPCServerService: CogRPCServer.Service
) {

    private val name = UUID.randomUUID().toString()

    val channel by lazy {
        InProcessChannelBuilder.forName(name).directExecutor().build()
    }
    val server by lazy {
        InProcessServerBuilder
            .forName(name)
            .addService(connectionService)
            .intercept(Authorization.interceptor)
            .directExecutor()
            .build()
    }
    val connectionService by lazy {
        CogRPCConnectionService(TestCoroutineScope(), cogRPCServerService)
    }

    fun start() {
        server.start()
    }

    fun stop() {
        channel.shutdown()
        server.shutdown()
    }
}
