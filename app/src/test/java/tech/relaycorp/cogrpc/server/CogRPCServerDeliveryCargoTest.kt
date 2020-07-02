package tech.relaycorp.cogrpc.server

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.internal.testing.StreamRecorder
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.cogrpc.server.DataFactory.buildDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import java.io.InputStream
import java.nio.charset.Charset

internal class CogRPCServerDeliveryCargoTest {
    @Test
    internal fun `deliverCargo with ack when successfully`() = runBlockingTest {
        val mockService = MockCogRPCServerService()
        val testServer = TestCogRPCServer(mockService)
        val clientStub = CargoRelayGrpc.newStub(testServer.channel)
        testServer.start()

        val ackRecorder = StreamRecorder.create<CargoDeliveryAck>()
        val deliveryObserver = clientStub.deliverCargo(ackRecorder)
        val delivery = buildDelivery()
        deliveryObserver.onNext(delivery)
        deliveryObserver.onCompleted()

        assertEquals(
            delivery.cargo.toString(Charset.defaultCharset()),
            mockService.deliverCargoCalls.last().readBytesAndClose()
                .toString(Charset.defaultCharset())
        )
        assertEquals(
            delivery.id,
            ackRecorder.values.first().id
        )

        testServer.stop()
    }

    @Test
    internal fun `deliverCargo without ack when no space available`() = runBlockingTest {
        val mockService = object : MockCogRPCServerService() {
            override suspend fun deliverCargo(cargoSerialized: InputStream): CogRPCServer.DeliverResult {
                super.deliverCargo(cargoSerialized)
                return CogRPCServer.DeliverResult.UnavailableStorage
            }
        }
        val testServer = TestCogRPCServer(mockService)
        val clientStub = CargoRelayGrpc.newStub(testServer.channel)
        testServer.start()

        val ackRecorder = StreamRecorder.create<CargoDeliveryAck>()
        val deliveryObserver = clientStub.deliverCargo(ackRecorder)
        val delivery = buildDelivery()
        deliveryObserver.onNext(delivery)
        deliveryObserver.onCompleted()

        assertEquals(
            delivery.cargo.toString(Charset.defaultCharset()),
            mockService.deliverCargoCalls.last().readBytesAndClose()
                .toString(Charset.defaultCharset())
        )
        assertTrue(ackRecorder.values.isEmpty())
        assertEquals(
            Status.RESOURCE_EXHAUSTED,
            (ackRecorder.error as StatusRuntimeException).status
        )

        testServer.stop()
    }

    @Test
    internal fun `deliverCargo with invalid cargo`() = runBlockingTest {
        val mockService = object : MockCogRPCServerService() {
            override suspend fun deliverCargo(cargoSerialized: InputStream): CogRPCServer.DeliverResult {
                super.deliverCargo(cargoSerialized)
                return CogRPCServer.DeliverResult.Invalid
            }
        }
        val testServer = TestCogRPCServer(mockService)
        val clientStub = CargoRelayGrpc.newStub(testServer.channel)
        testServer.start()

        val ackRecorder = StreamRecorder.create<CargoDeliveryAck>()
        val deliveryObserver = clientStub.deliverCargo(ackRecorder)
        val delivery = buildDelivery()
        deliveryObserver.onNext(delivery)
        deliveryObserver.onCompleted()

        assertEquals(
            delivery.cargo.toString(Charset.defaultCharset()),
            mockService.deliverCargoCalls.last().readBytesAndClose()
                .toString(Charset.defaultCharset())
        )
        assertTrue(ackRecorder.values.isEmpty())
        assertEquals(
            Status.INVALID_ARGUMENT,
            (ackRecorder.error as StatusRuntimeException).status
        )

        testServer.stop()
    }
}
