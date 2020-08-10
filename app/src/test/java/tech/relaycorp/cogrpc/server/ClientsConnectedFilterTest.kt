package tech.relaycorp.cogrpc.server

import io.grpc.Attributes
import io.grpc.Grpc
import io.netty.channel.local.LocalAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.test.WaitAssertions.waitForAssertEquals

internal class ClientsConnectedFilterTest {

    private val subject = ClientsConnectedFilter()

    @Test
    internal fun count() = runBlockingTest {
        val client1Attributes =
            Attributes.newBuilder().set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, LocalAddress("1")).build()
        val client2Attributes =
            Attributes.newBuilder().set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, LocalAddress("2")).build()

        waitForAssertEquals(0, subject.clientsCount()::first)
        subject.transportReady(client1Attributes)
        waitForAssertEquals(1, subject.clientsCount()::first)
        subject.transportReady(client1Attributes)
        waitForAssertEquals(1, subject.clientsCount()::first)
        subject.transportReady(client2Attributes)
        waitForAssertEquals(2, subject.clientsCount()::first)
        subject.transportTerminated(client1Attributes)
        waitForAssertEquals(1, subject.clientsCount()::first)
        subject.transportTerminated(client2Attributes)
        waitForAssertEquals(0, subject.clientsCount()::first)
    }
}
