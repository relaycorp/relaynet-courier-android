package tech.relaycorp.relaynet.cogrpc.server

import io.grpc.Context
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.netty.handler.codec.base64.Base64Decoder
import org.apache.commons.codec.binary.Base64

internal object Authorization {
    internal val metadataKey =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    internal val contextKey = Context.key<String>("Authorization")

    internal val interceptor by lazy {
        object : ServerInterceptor {
            override fun <ReqT : Any?, RespT : Any?> interceptCall(
                call: ServerCall<ReqT, RespT>,
                headers: Metadata,
                next: ServerCallHandler<ReqT, RespT>
            ): ServerCall.Listener<ReqT> {
                val auth = headers[metadataKey]
                val context = Context.current().withValue(contextKey, auth)
                val previousContext = context.attach()
                return try {
                    next.startCall(call, headers)
                } finally {
                    context.detach(previousContext)
                }
            }
        }
    }

    internal fun getCCA(): ByteArray {
        val auth = contextKey.get()
        if (auth == null || !auth.startsWith(CCA_HEADER)) {
            throw IllegalArgumentException("Cargo Collection Authorization not provided")
        }

        val ccaBase64 = auth.substring(CCA_HEADER.length)
        return Base64().decode(ccaBase64.toByteArray())
    }

    private const val CCA_HEADER = "Relaynet-CCA "
}
