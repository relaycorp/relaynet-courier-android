package tech.relaycorp.cogrpc.server

import io.grpc.internal.testing.StreamRecorder
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.cogrpc.server.CogRPCFactory.buildDeliveryAck
import tech.relaycorp.relaynet.cogrpc.Authorization
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.CogRPC
import java.util.concurrent.TimeUnit

internal class CogRPCServerCollectCargoTest {

    private lateinit var mockService: MockCogRPCServerService
    private lateinit var testServer: TestCogRPCServer

    @AfterEach
    internal fun tearDown() {
        testServer.stop()
    }

    @Test
    internal fun `collectCargo with correct CCA and all cargo acked`() = runBlockingTest {
        setupAndStartServer()

        val cca = "ABC".toByteArray()
        val authClientStub =
            Authorization.authorizeClientWithCCA(buildClientStub(), cca)

        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        val ackObserver = authClientStub.collectCargo(deliveryRecorder)
        val cargoReceived = deliveryRecorder.values.first()
        ackObserver.onNext(buildDeliveryAck(cargoReceived.id))

        assertEquals(
            cca.toList(),
            mockService.collectCargoCalls.last().data.readBytes().toList()
        )

        assertEquals(
            MockCogRPCServerService.CARGO_DELIVERED,
            cargoReceived.cargo
        )

        ackObserver.onNext(buildDeliveryAck(cargoReceived.id))
        ackObserver.onCompleted()
        assertEquals(
            cargoReceived.id,
            mockService.processCargoCollectionAckCalls.last().localId
        )

        assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
    }

    @Test
    internal fun `collectCargo with missing CCA`() = runBlockingTest {
        setupAndStartServer()

        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        buildClientStub().collectCargo(deliveryRecorder)

        assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
        assertTrue(deliveryRecorder.values.isEmpty())
    }

    @Test
    internal fun `collectCargo with invalid CCA`() = runBlockingTest {
        setupAndStartServer()

        val authClientStub =
            Authorization.authorizeClient(buildClientStub(), "INVALID")
        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        authClientStub.collectCargo(deliveryRecorder)

        assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
        assertTrue(deliveryRecorder.values.isEmpty())
    }

    @Test
    internal fun `collectCargo with no cargo to collect`() {
        setupAndStartServer(object : MockCogRPCServerService() {
            override suspend fun collectCargo(cca: CogRPC.MessageDelivery): Iterable<CogRPC.MessageDelivery> {
                super.collectCargo(cca)
                return emptyList()
            }
        })

        val cca = "CCA".toByteArray()
        val authClientStub = Authorization.authorizeClientWithCCA(buildClientStub(), cca)
        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        authClientStub.collectCargo(deliveryRecorder)

        assertEquals(
            cca.toList(),
            mockService.collectCargoCalls.last().data.readBytes().toList()
        )

        assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
        assertTrue(deliveryRecorder.values.isEmpty())
    }

    @Test
    internal fun `collectCargo without ack`() {
        setupAndStartServer()

        val authClientStub =
            Authorization.authorizeClientWithCCA(buildClientStub(), "CCA".toByteArray())
        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        authClientStub.collectCargo(deliveryRecorder)
        val cargoReceived = deliveryRecorder.values.first()

        // Got cargo but didn't ack
        assertEquals(
            MockCogRPCServerService.CARGO_DELIVERED,
            cargoReceived.cargo
        )

        // Stream does not end because it's waiting for an ack
        assertFalse(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
    }

    private fun setupAndStartServer(service: MockCogRPCServerService = MockCogRPCServerService()) {
        mockService = service
        testServer = TestCogRPCServer(mockService)
        testServer.start()
    }

    private fun buildClientStub() = CargoRelayGrpc.newStub(testServer.channel)
}
