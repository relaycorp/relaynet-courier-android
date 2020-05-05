package tech.relaycorp.relaynet.cogrpc

import io.grpc.Metadata
import org.apache.commons.codec.binary.Base64
import java.nio.charset.Charset

object AuthorizationMetadata {

    internal val MetadataKey =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    internal const val AUTHORIZATION_TYPE = "Relaynet-CCA "

    fun makeMetadata(ccaSerialized: ByteArray) =
        Metadata().also {
            it.put(MetadataKey, encodeAuthValue(ccaSerialized))
        }

    fun getCCASerialized(metadata: Metadata) =
        decodeAuthValue(metadata[MetadataKey])

    private fun encodeAuthValue(cca: ByteArray) =
        AUTHORIZATION_TYPE + Base64().encode(cca).toString(Charset.defaultCharset())

    private fun decodeAuthValue(authMetadata: String?): ByteArray? {
        if (authMetadata?.startsWith(AUTHORIZATION_TYPE) != true) return null

        val ccaBase64 = authMetadata.substring(AUTHORIZATION_TYPE.length)
        return Base64().decode(ccaBase64.toByteArray())
    }
}
