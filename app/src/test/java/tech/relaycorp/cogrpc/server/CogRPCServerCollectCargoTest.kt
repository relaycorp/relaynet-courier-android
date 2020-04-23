package tech.relaycorp.cogrpc.server

import io.grpc.internal.testing.StreamRecorder
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.cogrpc.server.CogRPCFactory.buildDeliveryAck
import tech.relaycorp.cogrpc.server.CogRPCFactory.wrapClientWithAuth
import tech.relaycorp.cogrpc.server.CogRPCFactory.wrapClientWithCCA
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.CogRPC
import java.nio.charset.Charset
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

        val cca = "ABC"
        val authClientStub = wrapClientWithCCA(buildClientStub(), cca)

        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        val ackObserver = authClientStub.collectCargo(deliveryRecorder)
        val cargoReceived = deliveryRecorder.values.first()
        ackObserver.onNext(buildDeliveryAck(cargoReceived.id))

        assertEquals(
            cca,
            mockService.collectCargoCalls.last().data.readBytes().toString(Charset.defaultCharset())
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

        val authClientStub = wrapClientWithAuth(buildClientStub(), "INVALID")
        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        authClientStub.collectCargo(deliveryRecorder)

        assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
        assertTrue(deliveryRecorder.values.isEmpty())
    }

    @Test
    internal fun `collectCargo with no cargo to collect`() {
        setupAndStartServer(object : MockCogRPCServerService() {
            override suspend fun collectCargo(cca: CogRPC.MessageReceived): Iterable<CogRPC.MessageDelivery> {
                super.collectCargo(cca)
                return emptyList()
            }
        })

        val cca = "CCA"
        val authClientStub = wrapClientWithCCA(buildClientStub(), cca)
        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        authClientStub.collectCargo(deliveryRecorder)

        assertEquals(
            cca,
            mockService.collectCargoCalls.last().data.readBytes().toString(Charset.defaultCharset())
        )

        assertTrue(deliveryRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
        assertTrue(deliveryRecorder.values.isEmpty())
    }

    @Test
    internal fun `collectCargo without ack`() {
        setupAndStartServer()

        val authClientStub = wrapClientWithCCA(buildClientStub())
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
