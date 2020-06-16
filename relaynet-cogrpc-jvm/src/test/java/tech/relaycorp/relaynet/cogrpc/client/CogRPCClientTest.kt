package tech.relaycorp.relaynet.cogrpc.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.internal.testing.StreamRecorder
import io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.test.MockCogRPCServerService
import tech.relaycorp.relaynet.cogrpc.test.NoopStreamObserver
import tech.relaycorp.relaynet.cogrpc.test.TestCogRPCServer
import tech.relaycorp.relaynet.cogrpc.test.Wait.waitForNotNull
import tech.relaycorp.relaynet.cogrpc.toAck
import tech.relaycorp.relaynet.cogrpc.toCargoDelivery
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class CogRPCClientTest {

    private var testServer: TestCogRPCServer? = null

    @AfterEach
    internal fun tearDown() {
        testServer?.stop()
        testServer = null
    }

    @Nested
    inner class Build {
        private val spiedChannelBuilder: NettyChannelBuilder =
            spy(NettyChannelBuilder.forAddress(InetSocketAddress(80)))

        private val spiedChannelBuilderProvider: (InetSocketAddress) -> NettyChannelBuilder =
            { spiedChannelBuilder }

        @Test
        internal fun `invalid address throws exception`() {
            assertThrows<MalformedURLException> { CogRPCClient.Builder.build("invalid") }
        }

        @Test
        internal fun `TLS is required by default`() {
            val client = CogRPCClient.Builder.build("https://example.org")
            assertTrue(client.requireTls)
        }

        @Test
        internal fun `HTTPS URL defaults to port 443`() {
            val client = CogRPCClient.Builder.build("https://example.org")

            assertEquals("example.org:443", client.channel.authority())
        }

        @Test
        internal fun `HTTP URL with TLS required should throw an exception`() {
            val serverAddress = "http://example.com"
            val exception =
                assertThrows<CogRPCClient.CogRPCException> {
                    CogRPCClient.Builder.build(
                        serverAddress
                    )
                }

            assertEquals("Cannot connect to $serverAddress with TLS required", exception.message)
        }

        @Test
        internal fun `HTTP URL defaults to port 80`() {
            val client = CogRPCClient.Builder.build("http://example.org", false)

            assertEquals("example.org:80", client.channel.authority())
        }

        @Test
        internal fun `Channel should use TLS if URL is HTTPS`() {
            val spiedClient =
                spy(CogRPCClient("https://1.1.1.1", true, spiedChannelBuilderProvider))

            assertTrue(spiedClient.channel is ManagedChannel)
            verify(spiedChannelBuilder, never()).usePlaintext()
        }

        @Test
        internal fun `Channel should use TLS if TLS is not required but URL is HTTPS`() {
            val spiedClient = spy(CogRPCClient("https://1.1.1.1", false, spiedChannelBuilderProvider))

            assertTrue(spiedClient.channel is ManagedChannel)
            verify(spiedChannelBuilder).useTransportSecurity()
            verify(spiedChannelBuilder, never()).usePlaintext()
        }

        @Test
        internal fun `TLS server certificate should be validated if host is not private IP`() {
            val spiedClient = spy(CogRPCClient("https://1.1.1.1", true, spiedChannelBuilderProvider))

            assertTrue(spiedClient.channel is ManagedChannel)
            verify(spiedChannelBuilder, never()).usePlaintext()
            verify(spiedChannelBuilder, never()).sslContext(any())
        }

        @Test
        internal fun `TLS server certificate should not be validated if host is private IP`() {
            val spiedClient = spy(CogRPCClient("https://192.168.43.1", true, spiedChannelBuilderProvider))

            assertTrue(spiedClient.channel is ManagedChannel)
            verify(spiedChannelBuilder, never()).usePlaintext()
            verify(spiedChannelBuilder).sslContext(spiedClient.insecureTlsContext)
        }

        @Test
        internal fun `TLS should not be used if URL is HTTP`() {
            val spiedClient = spy(CogRPCClient("http://192.168.43.1", false, spiedChannelBuilderProvider))

            assertTrue(spiedClient.channel is ManagedChannel)
            verify(spiedChannelBuilder).usePlaintext()
            verify(spiedChannelBuilder, never()).sslContext(any())
        }
    }

    @Test
    internal fun `deliver cargo and receive ack`() = runBlocking {
        val mockServerService = MockCogRPCServerService()
        buildAndStartServer(mockServerService)
        val client = CogRPCClient.Builder.build(ADDRESS, false)
        val cargo = buildRequest()

        // Server acks and completes instantaneously
        mockServerService.deliverCargoReturned = object : NoopStreamObserver<CargoDelivery>() {
            override fun onNext(value: CargoDelivery) {
                mockServerService.deliverCargoReceived?.onNext(cargo.toCargoDelivery().toAck())
            }

            override fun onCompleted() {
                mockServerService.deliverCargoReceived?.onCompleted()
            }
        }

        val ackFlow = client.deliverCargo(listOf(cargo))

        assertEquals(
            cargo.localId,
            ackFlow.first()
        )

        client.close()
        testServer?.stop()
    }

    @Test
    internal fun `deliver cargo throws exception if deadline is exceeded`() = runBlocking {
        val mockServerService = MockCogRPCServerService()
        buildAndStartServer(mockServerService)
        val client = CogRPCClient.Builder.build(ADDRESS, false)
        val cargo = buildRequest()

        // Server acks and completes instantaneously
        mockServerService.deliverCargoReturned = object : NoopStreamObserver<CargoDelivery>() {
            override fun onNext(value: CargoDelivery) {
                @Suppress("BlockingMethodInNonBlockingContext")
                Thread.sleep(CogRPCClient.CALL_DEADLINE.inMilliseconds.toLong())
                mockServerService.deliverCargoReceived?.onNext(cargo.toCargoDelivery().toAck())
            }
        }

        val exception = assertThrows<CogRPCClient.CogRPCException> {
            runBlocking {
                client.deliverCargo(listOf(cargo)).collect()
            }
        }
        assertEquals(StatusRuntimeException::class, exception.cause!!::class)
        assertEquals(
            Status.DEADLINE_EXCEEDED.code,
            (exception.cause as StatusRuntimeException).status.code
        )

        client.close()
        testServer?.stop()
    }

    @Test
    internal fun `collect cargo, ack and complete`() = runBlocking {
        val mockServerService = MockCogRPCServerService()
        buildAndStartServer(mockServerService)
        val client = CogRPCClient.Builder.build(ADDRESS, false)
        val ackRecorder = StreamRecorder.create<CargoDeliveryAck>()
        mockServerService.collectCargoReturned = ackRecorder

        // Client call
        val cca = buildMessageSerialized()
        val collectFlow = client.collectCargo { cca }

        // Server sends cargo
        val cargo = buildRequest()
        launch(Dispatchers.IO) {
            waitForNotNull { mockServerService.collectCargoReceived }
                .onNext(cargo.toCargoDelivery())
        }

        assertNotNull(collectFlow.first())

        assertEquals(
            cargo.localId,
            ackRecorder.values.first().id
        )

        mockServerService.collectCargoReceived?.onCompleted()
        assertTrue(ackRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))

        client.close()
        testServer?.stop()
    }

    @Test
    internal fun `collect cargo throws exception if deadline is exceeded`() = runBlocking {
        val mockServerService = MockCogRPCServerService()
        buildAndStartServer(mockServerService)
        val client = CogRPCClient.Builder.build(ADDRESS, false)
        val ackRecorder = StreamRecorder.create<CargoDeliveryAck>()
        mockServerService.collectCargoReturned = ackRecorder

        // Client call
        val cca = buildMessageSerialized()
        val collectFlow = client.collectCargo { cca }

        // Server sends cargo
        val cargo = buildRequest()
        launch(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            Thread.sleep(CogRPCClient.CALL_DEADLINE.inMilliseconds.toLong())
            waitForNotNull { mockServerService.collectCargoReceived }
                .onNext(cargo.toCargoDelivery())
        }

        val exception = assertThrows<CogRPCClient.CogRPCException> {
            runBlocking {
                collectFlow.collect()
            }
        }
        assertEquals(StatusRuntimeException::class, exception.cause!!::class)
        assertEquals(
            Status.DEADLINE_EXCEEDED.code,
            (exception.cause as StatusRuntimeException).status.code
        )

        client.close()
        testServer?.stop()
    }

    private fun buildAndStartServer(service: BindableService) {
        testServer = TestCogRPCServer(HOST, PORT, service).apply { start() }
    }

    private fun buildMessageSerialized() =
        "DATA".byteInputStream()

    private fun buildRequest() =
        CargoDeliveryRequest(
            UUID.randomUUID().toString()
        ) { buildMessageSerialized() }

    companion object {
        private const val HOST = "localhost"
        private const val PORT = 8080
        private const val ADDRESS = "http://$HOST:$PORT"
    }
}
