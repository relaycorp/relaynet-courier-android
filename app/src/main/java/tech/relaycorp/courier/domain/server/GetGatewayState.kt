package tech.relaycorp.courier.domain.server

import tech.relaycorp.cogrpc.server.GatewayIPAddressException
import tech.relaycorp.courier.GetGatewayIpAddress
import javax.inject.Inject

class GetGatewayState @Inject constructor(
    @GetGatewayIpAddress private val getGatewayIpAddress: () -> String
) {

    operator fun invoke(): GatewayState = try {
        getGatewayIpAddress()
        GatewayState.Available
    } catch (e: GatewayIPAddressException) {
        GatewayState.Unavailable
    }

    enum class GatewayState {
        Available, Unavailable
    }
}
