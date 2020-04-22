package tech.relaycorp.cogrpc.server

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.internal.testing.StreamRecorder
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import java.nio.charset.Charset

internal class CogRPCConnectionServiceTest {

    private lateinit var testService: TestCogRPCServerService
    private lateinit var channel: ManagedChannel
    private lateinit var server: Server
    private lateinit var subject: CogRPCConnectionService
    private val clientStub get() = CargoRelayGrpc.newStub(channel)

    @BeforeEach
    internal fun setUp() {
        testService = TestCogRPCServerService()
        subject = CogRPCConnectionService(TestCoroutineScope(), testService)
        channel = InProcessChannelBuilder.forName("test").directExecutor().build()
        server = InProcessServerBuilder
            .forName("test")
            .addService(subject)
            .intercept(Authorization.interceptor)
            .directExecutor()
            .build()
            .start()
    }

    @AfterEach
    internal fun tearDown() {
        channel.shutdown()
        server.shutdown()
    }

    @Test
    internal fun deliverCargo() = runBlockingTest {
        val ackRecorder = StreamRecorder.create<CargoDeliveryAck>()
        val deliveryObserver = clientStub.deliverCargo(ackRecorder)
        val delivery = buildDelivery()
        deliveryObserver.onNext(delivery)
        deliveryObserver.onCompleted()

        assertEquals(
            delivery.cargo.toString(Charset.defaultCharset()),
            testService.deliverCargoCalls.last().data.readBytes().toString(Charset.defaultCharset())
        )
        assertEquals(
            delivery.id,
            ackRecorder.values.first().id
        )
    }

    @Test
    internal fun collectCargo() = runBlockingTest {
        val cca = "ABC"
        val ccaEncoded = Base64().encode(cca.toByteArray())
        val authClientStub = MetadataUtils.attachHeaders(
            clientStub,
            Metadata().also {
                it.put(
                    Authorization.metadataKey,
                    Authorization.AUTHORIZATION_TYPE + ccaEncoded.toString(Charset.defaultCharset())
                )
            }
        )

        val deliveryRecorder = StreamRecorder.create<CargoDelivery>()
        val ackObserver = authClientStub.collectCargo(deliveryRecorder)
        val cargoReceived = deliveryRecorder.values.first()
        ackObserver.onNext(buildDeliveryAck(cargoReceived.id))

        assertEquals(
            cca,
            testService.collectCargoCalls.last().data.readBytes().toString(Charset.defaultCharset())
        )

        assertEquals(
            TestCogRPCServerService.CARGO_DELIVERED,
            cargoReceived.cargo
        )

        ackObserver.onNext(buildDeliveryAck(cargoReceived.id))
        ackObserver.onCompleted()
        assertEquals(
            cargoReceived.id,
            testService.processCargoCollectionAckCalls.last().localId
        )
    }

    private fun buildDelivery() =
        CargoDelivery.newBuilder()
            .setId("1234")
            .setCargo(ByteString.copyFromUtf8("ABC"))
            .build()

    private fun buildDeliveryAck(id: String) =
        CargoDeliveryAck.newBuilder().setId(id).build()
}
