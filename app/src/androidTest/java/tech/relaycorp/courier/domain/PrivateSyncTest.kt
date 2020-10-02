package tech.relaycorp.courier.domain

import io.grpc.internal.testing.StreamRecorder
import io.grpc.stub.MetadataUtils
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import tech.relaycorp.cogrpc.okhttp.OkHTTPChannelBuilderProvider
import tech.relaycorp.cogrpc.server.Networking
import tech.relaycorp.courier.test.appComponent
import tech.relaycorp.relaynet.cogrpc.AuthorizationMetadata
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.client.PrivateSubnetTrustManager
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PrivateSyncTest {

    @Inject
    lateinit var privateSync: PrivateSync

    @Before
    fun setUp() {
        appComponent.inject(this)
    }

    @Test
    fun privateSync() {
        Networking.androidGatewaySubnetPrefix = null
        runBlocking { privateSync.startSync() }

        val deliveryStream = StreamRecorder.create<CargoDelivery>()
        client.collectCargo(deliveryStream)

        deliveryStream.awaitCompletion(2, TimeUnit.SECONDS)
        assertTrue(deliveryStream.values.isEmpty())
        assertNull(deliveryStream.error)

        clientChannel.shutdown()
        runBlocking { privateSync.stopSync() }
    }

    private val clientChannel by lazy {
        val gatewayIpAddress = Networking.getGatewayIpAddress()
        OkHTTPChannelBuilderProvider
            .makeBuilder(
                InetSocketAddress(gatewayIpAddress, 21473),
                PrivateSubnetTrustManager.INSTANCE
            )
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private val client by lazy {
        MetadataUtils.attachHeaders(
            CargoRelayGrpc.newStub(clientChannel),
            AuthorizationMetadata.makeMetadata("CCA".toByteArray())
        )
    }
}
