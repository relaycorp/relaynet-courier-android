package tech.relaycorp.relaynet.cogrpc.client

import io.grpc.BindableService
import io.grpc.internal.testing.StreamRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CogRPC
import tech.relaycorp.relaynet.cogrpc.test.MockCogRPCServerService
import tech.relaycorp.relaynet.cogrpc.test.TestCogRPCServer
import tech.relaycorp.relaynet.cogrpc.test.Wait.waitForNotNull
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class CogRPCClientTest {

    private var testServer: TestCogRPCServer? = null

    @AfterEach
    internal fun tearDown() {
        testServer?.stop()
        testServer = null
    }

    @Test
    internal fun `collect cargo, ack and complete`() = runBlocking {
        val mockServerService = MockCogRPCServerService()
        buildAndStartServer(mockServerService)
        val client = CogRPCClient(ADDRESS, false)
        val ackRecorder = StreamRecorder.create<CargoDeliveryAck>()
        mockServerService.collectCargoReturned = ackRecorder

        // Client call
        val cca = buildMessageDelivery()
        val collectFlow = client.collectCargo(cca)

        // Server sends cargo
        val cargo = buildMessageDelivery()
        launch(Dispatchers.IO) {
            waitForNotNull { mockServerService.collectCargoReceived }
                .onNext(cargo.toCargoDelivery())
        }

        assertEquals(
            cargo.localId,
            collectFlow.first().localId
        )

        assertEquals(
            cargo.localId,
            ackRecorder.values.first().id
        )

        mockServerService.collectCargoReceived?.onCompleted()
        assertTrue(ackRecorder.awaitCompletion(100, TimeUnit.MILLISECONDS))
    }

    @Test
    internal fun `deliver cargo and receive ack`() = runBlocking {
        val mockServerService = MockCogRPCServerService()
        buildAndStartServer(mockServerService)
        val client = CogRPCClient(ADDRESS, false)
        val cargo = buildMessageDelivery()

        // Server acks as soon as it receives cargo
        launch(Dispatchers.IO) {
            val ackObserver = waitForNotNull { mockServerService.deliverCargoReceived }
            ackObserver.onNext(cargo.toCargoDeliveryAck())
        }

        val ackFlow = client.deliverCargo(listOf(cargo))

        assertEquals(
            cargo.localId,
            ackFlow.first().localId
        )
    }

    private fun buildAndStartServer(service: BindableService) {
        testServer = TestCogRPCServer(HOST, PORT, service).apply { start() }
    }

    private fun buildMessageDelivery() =
        CogRPC.MessageDelivery(UUID.randomUUID().toString(), "DATA".byteInputStream())

    companion object {
        private const val HOST = "localhost"
        private const val PORT = 8080
        private const val ADDRESS = "http://$HOST:$PORT"
    }
}
