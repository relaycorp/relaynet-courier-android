package tech.relaycorp.cogrpc.server

import io.grpc.Metadata
import io.grpc.internal.testing.StreamRecorder
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tech.relaycorp.cogrpc.server.DataFactory.buildDeliveryAck
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.AuthorizationMetadata
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.toCargoDeliveryAck
import java.util.concurrent.TimeUnit

class CogRPCServerCollectCargoTest {
    private lateinit var mockService: MockCogRPCServerService
    private lateinit var testServer: TestCogRPCServer

    @AfterEach
    internal fun tearDown() {
        testServer.stop()
    }

    @Test
    @Disabled("See PR #610")
    internal fun `collectCargo with correct CCA and all cargo acked`() =
        runBlockingTest {
            setupAndStartServer()

            val cca = "ABC".toByteArray()
            val authClient = buildClientWithCCA(cca)

            val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
            val ackObserver = authClient.collectCargo(deliveryRecorder)
            val cargoReceived = deliveryRecorder.values.first()
            ackObserver.onNext(cargoReceived.id.toCargoDeliveryAck())

            assertEquals(
                cca.toList(),
                mockService.collectCargoCalls.last().toList(),
            )

            assertEquals(
                MockCogRPCServerService.CARGO_DELIVERED,
                cargoReceived.cargo,
            )

            ackObserver.onNext(buildDeliveryAck(cargoReceived.id))
            ackObserver.onCompleted()
            assertEquals(
                cargoReceived.id,
                mockService.processCargoCollectionAckCalls.last(),
            )

            assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
        }

    @Test
    internal fun `collectCargo with missing CCA`() =
        runBlockingTest {
            setupAndStartServer()

            val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
            buildClient().collectCargo(deliveryRecorder)

            assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
            assertTrue(deliveryRecorder.values.isEmpty())
        }

    @Test
    internal fun `collectCargo with invalid CCA`() =
        runBlockingTest {
            setupAndStartServer()

            val invalidAuthClient = buildClientWithInvalidAuthorization()
            val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
            invalidAuthClient.collectCargo(deliveryRecorder)

            assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
            assertTrue(deliveryRecorder.values.isEmpty())
        }

    @Test
    internal fun `collectCargo with no cargo to collect`() {
        setupAndStartServer(
            object : MockCogRPCServerService() {
                override suspend fun collectCargo(ccaSerialized: ByteArray): Iterable<CargoDeliveryRequest> {
                    super.collectCargo(ccaSerialized)
                    return emptyList()
                }
            },
        )

        val cca = "CCA".toByteArray()
        val authClient = buildClientWithCCA(cca)
        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        authClient.collectCargo(deliveryRecorder)

        assertEquals(
            cca.toList(),
            mockService.collectCargoCalls.last().toList(),
        )

        assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
        assertTrue(deliveryRecorder.values.isEmpty())
    }

    @Test
    @Disabled("See PR #610")
    fun `collectCargo without ack`() {
        setupAndStartServer()

        val authClient = buildClientWithCCA()
        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        authClient.collectCargo(deliveryRecorder)
        val cargoReceived = deliveryRecorder.values.first()

        // Got cargo but didn't ack
        assertEquals(
            MockCogRPCServerService.CARGO_DELIVERED,
            cargoReceived.cargo,
        )

        // Stream does not end because it's waiting for an ack
        assertFalse(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
    }

    private fun setupAndStartServer(service: MockCogRPCServerService = MockCogRPCServerService()) {
        mockService = service
        testServer = TestCogRPCServer(mockService)
        testServer.start()
    }

    private fun buildClient() = CargoRelayGrpc.newStub(testServer.channel)

    private fun buildClientWithCCA(cca: ByteArray = "CCA".toByteArray()) =
        buildClient().withInterceptors(
            MetadataUtils.newAttachHeadersInterceptor(AuthorizationMetadata.makeMetadata(cca)),
        )

    private fun buildClientWithInvalidAuthorization() =
        buildClient().withInterceptors(
            MetadataUtils.newAttachHeadersInterceptor(
                Metadata().also {
                    it.put(
                        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                        "INVALID",
                    )
                },
            ),
        )
}
