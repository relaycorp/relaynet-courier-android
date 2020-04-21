package tech.relaycorp.cogrpc.server

import io.grpc.Attributes
import io.grpc.Grpc
import io.grpc.ServerTransportFilter
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import tech.relaycorp.courier.common.BehaviorChannel

class ClientsConnectedFilter : ServerTransportFilter() {

    private val clients = BehaviorChannel(emptyList<String>())
    fun clientsCount() = clients.asFlow().map { it.size }

    override fun transportReady(transportAttrs: Attributes): Attributes {
        transportAttrs.calledIpAddress?.let {
            clients.sendBlocking((clients.value + it).distinct())
        }
        return super.transportReady(transportAttrs)
    }

    override fun transportTerminated(transportAttrs: Attributes) {
        transportAttrs.calledIpAddress?.let {
            clients.sendBlocking(clients.value - it)
        }
        super.transportTerminated(transportAttrs)
    }

    private val Attributes.calledIpAddress
        get() = this[Grpc.TRANSPORT_ATTR_REMOTE_ADDR]?.toString()
}
