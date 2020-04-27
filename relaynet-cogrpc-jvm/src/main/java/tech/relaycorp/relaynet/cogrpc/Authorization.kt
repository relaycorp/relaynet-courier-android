package tech.relaycorp.relaynet.cogrpc

import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.apache.commons.codec.binary.Base64
import java.nio.charset.Charset

object Authorization {

    internal val MetadataKey =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    internal const val AUTHORIZATION_TYPE = "Relaynet-CCA "

    fun authorizeClientWithCCA(client: CargoRelayGrpc.CargoRelayStub, cca: ByteArray) =
        authorizeClient(client, encodeAuthValue(cca))

    fun authorizeClient(client: CargoRelayGrpc.CargoRelayStub, authorization: String) =
        MetadataUtils.attachHeaders(
            client,
            Metadata().also {
                it.put(MetadataKey, authorization)
            }
        )

    fun getClientCCA(headers: Metadata) =
        decodeAuthValue(headers[MetadataKey])

    private fun decodeAuthValue(authMetadata: String?): ByteArray? {
        if (authMetadata?.startsWith(AUTHORIZATION_TYPE) != true) return null

        val ccaBase64 = authMetadata.substring(AUTHORIZATION_TYPE.length)
        return Base64().decode(ccaBase64.toByteArray())
    }

    private fun encodeAuthValue(cca: ByteArray) =
        AUTHORIZATION_TYPE + Base64().encode(cca).toString(Charset.defaultCharset())
}
