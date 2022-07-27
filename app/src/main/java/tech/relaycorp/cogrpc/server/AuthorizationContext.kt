package tech.relaycorp.cogrpc.server

import io.grpc.Context
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import tech.relaycorp.relaynet.cogrpc.AuthorizationMetadata

internal object AuthorizationContext {
    // Context values are bound to the current thread
    internal val contextKey: Context.Key<ByteArray> = Context.key("Authorization")

    internal val interceptor by lazy {
        object : ServerInterceptor {
            override fun <ReqT : Any?, RespT : Any?> interceptCall(
                call: ServerCall<ReqT, RespT>,
                headers: Metadata,
                next: ServerCallHandler<ReqT, RespT>
            ): ServerCall.Listener<ReqT> {
                val auth = AuthorizationMetadata.getCCASerialized(headers)
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

    internal fun getCCA(): ByteArray? = contextKey.get()
}
