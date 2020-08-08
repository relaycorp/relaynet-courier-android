package tech.relaycorp.cogrpc.server

import io.grpc.Attributes
import io.grpc.Grpc
import io.netty.channel.local.LocalAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ClientsConnectedFilterTest {

    private val subject = ClientsConnectedFilter()

    @Test
    fun `wrong 1`() {
        assertEquals(1, 2)
    }

    @Test
    internal fun count() = runBlockingTest {
        val client1Attributes =
            Attributes.newBuilder().set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, LocalAddress("1")).build()
        val client2Attributes =
            Attributes.newBuilder().set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, LocalAddress("2")).build()

        assertEquals(0, subject.clientsCount().first())
        subject.transportReady(client1Attributes)
        assertEquals(1, subject.clientsCount().first())
        subject.transportReady(client1Attributes)
        assertEquals(1, subject.clientsCount().first())
        subject.transportReady(client2Attributes)
        assertEquals(2, subject.clientsCount().first())
        subject.transportTerminated(client1Attributes)
        assertEquals(1, subject.clientsCount().first())
        subject.transportTerminated(client2Attributes)
        assertEquals(0, subject.clientsCount().first())
    }
}
