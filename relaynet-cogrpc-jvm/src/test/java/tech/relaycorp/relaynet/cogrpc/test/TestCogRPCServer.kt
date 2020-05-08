package tech.relaycorp.relaynet.cogrpc.test

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import java.net.InetSocketAddress

class TestCogRPCServer(
    private val host: String,
    private val port: Int,
    private val service: BindableService
) {
    private var server: Server? = null

    fun start() {
        server = NettyServerBuilder
            .forAddress(InetSocketAddress(host, port))
            .addService(service)
            .build()
            .start()
    }

    fun stop() {
        server?.shutdown()
        server = null
    }
}
