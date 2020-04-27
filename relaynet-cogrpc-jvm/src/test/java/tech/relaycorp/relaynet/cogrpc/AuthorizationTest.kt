package tech.relaycorp.relaynet.cogrpc

import io.grpc.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.relaynet.cogrpc.test.MockManagedChannel
import tech.relaycorp.relaynet.cogrpc.test.NoopStreamObserver
import java.nio.charset.Charset

internal class AuthorizationTest {

    @Test
    fun authorizeClient() {
        val authorization = "abc"
        val mockChannel =
            MockManagedChannel()
        val client = Authorization.authorizeClient(buildClient(mockChannel), authorization)

        client.collectCargo(NoopStreamObserver())

        assertEquals(
            authorization,
            mockChannel.lastCallMetadata?.get(Authorization.MetadataKey)
        )
    }

    @Test
    fun authorizeClientWithCCA() {
        val cca = "abc"
        val mockChannel =
            MockManagedChannel()
        val client =
            Authorization.authorizeClientWithCCA(buildClient(mockChannel), cca.toByteArray())

        client.collectCargo(NoopStreamObserver())
        val callMetadata = mockChannel.lastCallMetadata!!

        assertEquals(
            cca,
            Authorization.getClientCCA(callMetadata)?.toString(Charset.defaultCharset())
        )
    }

    private fun buildClient(channel: Channel) =
        CargoRelayGrpc.newStub(channel)
}
