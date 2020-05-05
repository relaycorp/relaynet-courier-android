package tech.relaycorp.relaynet.cogrpc.client

import io.grpc.BindableService
import io.grpc.internal.testing.StreamRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.test.MockCogRPCServerService
import tech.relaycorp.relaynet.cogrpc.test.NoopStreamObserver
import tech.relaycorp.relaynet.cogrpc.test.TestCogRPCServer
import tech.relaycorp.relaynet.cogrpc.test.Wait.waitForNotNull
import tech.relaycorp.relaynet.cogrpc.toAck
import tech.relaycorp.relaynet.cogrpc.toCargoDelivery
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
        val cca = buildMessageSerialized()
        val collectFlow = client.collectCargo(cca)

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
    }

    @Test
    internal fun `deliver cargo and receive ack`() = runBlocking {
        val mockServerService = MockCogRPCServerService()
        buildAndStartServer(mockServerService)
        val client = CogRPCClient(ADDRESS, false)
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
    }

    private fun buildAndStartServer(service: BindableService) {
        testServer = TestCogRPCServer(HOST, PORT, service).apply { start() }
    }

    private fun buildMessageSerialized() =
        "DATA".byteInputStream()

    private fun buildRequest() =
        CargoDeliveryRequest(UUID.randomUUID().toString(), buildMessageSerialized())

    companion object {
        private const val HOST = "localhost"
        private const val PORT = 8080
        private const val ADDRESS = "http://$HOST:$PORT"
    }
}
