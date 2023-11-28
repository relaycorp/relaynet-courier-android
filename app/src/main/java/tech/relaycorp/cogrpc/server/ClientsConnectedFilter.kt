package tech.relaycorp.cogrpc.server

import io.grpc.Attributes
import io.grpc.Grpc
import io.grpc.ServerTransportFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ClientsConnectedFilter : ServerTransportFilter() {
    private val clients = MutableStateFlow(emptyList<String>())

    fun clientsCount() = clients.asStateFlow().map { it.size }

    override fun transportReady(transportAttrs: Attributes): Attributes {
        transportAttrs.calledIpAddress?.let {
            clients.tryEmit((clients.value + it).distinct())
        }
        return super.transportReady(transportAttrs)
    }

    override fun transportTerminated(transportAttrs: Attributes?) {
        // transportAttrs == null when the incoming TLS connection is not HTTP2. This can be the
        // case with JS gRPC clients employing this workaround:
        // https://github.com/grpc/grpc-node/issues/663#issuecomment-624000152
        transportAttrs?.calledIpAddress?.let {
            clients.tryEmit(clients.value - it)
        }
        super.transportTerminated(transportAttrs)
    }

    private val Attributes.calledIpAddress
        get() = this[Grpc.TRANSPORT_ATTR_REMOTE_ADDR]?.toString()
}
