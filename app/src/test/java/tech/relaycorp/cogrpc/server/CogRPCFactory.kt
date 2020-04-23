package tech.relaycorp.cogrpc.server

import com.google.protobuf.ByteString
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.apache.commons.codec.binary.Base64
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck
import tech.relaycorp.relaynet.cogrpc.CargoRelayGrpc
import java.nio.charset.Charset

object CogRPCFactory {

    fun buildDelivery(id: String = "1234", cargo: String = "ABC") =
        CargoDelivery.newBuilder()
            .setId(id)
            .setCargo(ByteString.copyFromUtf8(cargo))
            .build()

    fun buildDeliveryAck(id: String) =
        CargoDeliveryAck.newBuilder()
            .setId(id)
            .build()

    fun wrapClientWithCCA(client: CargoRelayGrpc.CargoRelayStub, cca: String = "CCA") =
        wrapClientWithAuth(client, buildCCAMetadata(cca))

    fun wrapClientWithAuth(client: CargoRelayGrpc.CargoRelayStub, auth: String) =
        MetadataUtils.attachHeaders(
            client,
            Metadata().also {
                it.put(Authorization.metadataKey, auth)
            }
        )

    private fun buildCCAMetadata(cca: String) =
        Authorization.AUTHORIZATION_TYPE +
            Base64().encode(cca.toByteArray()).toString(Charset.defaultCharset())
}
