package tech.relaycorp.relaynet.cogrpc.test

import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.internal.NoopClientCall
import java.util.concurrent.TimeUnit

class MockManagedChannel : ManagedChannel() {

    var lastCallMetadata: Metadata? = null
        private set

    override fun <RequestT : Any?, ResponseT : Any?> newCall(
        methodDescriptor: MethodDescriptor<RequestT, ResponseT>?,
        callOptions: CallOptions?
    ): ClientCall<RequestT, ResponseT> {
        return object : NoopClientCall<RequestT, ResponseT>() {
            override fun start(listener: Listener<ResponseT>?, headers: Metadata?) {
                lastCallMetadata = headers
                super.start(listener, headers)
            }
        }
    }

    override fun isTerminated() = false

    override fun authority() = ""

    override fun shutdown() = this

    override fun isShutdown() = false

    override fun shutdownNow() = this

    override fun awaitTermination(timeout: Long, unit: TimeUnit?) = true
}
