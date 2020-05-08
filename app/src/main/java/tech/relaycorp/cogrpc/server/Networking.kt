package tech.relaycorp.cogrpc.server

import java.net.NetworkInterface

internal object Networking {
    private const val androidGatewaySubnetPrefix = "192.168.43."

    /**
     * Return the local IP address used by the current device in the WiFi hotspot network it created
     *
     * An exception may be thrown in rooted devices or those with a custom version of Android where
     * the number of local IP addresses in the subnet 192.168.43.0/24 does not equal one.
     */
    @Throws(GatewayIPAddressException::class)
    fun getGatewayIpAddress(): String {
        val localAddresses = getAllLocalIpAddresses()
        val gatewayAddresses =
            localAddresses.filter { it.startsWith(androidGatewaySubnetPrefix) }
        if (gatewayAddresses.isEmpty()) {
            throw GatewayIPAddressException(
                "No address in the subnet ${androidGatewaySubnetPrefix}0/24 was found"
            )
        } else if (gatewayAddresses.size > 1) {
            throw GatewayIPAddressException(
                "Multiple addresses in the subnet ${androidGatewaySubnetPrefix}0/24 were found"
            )
        }
        return gatewayAddresses.first()
    }

    fun getAllLocalIpAddresses(): List<String> {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces().iterator().asSequence()
        val localAddresses = networkInterfaces.flatMap {
            it.inetAddresses.asSequence()
                .filter { inetAddress -> inetAddress.isSiteLocalAddress }
                .map { inetAddress -> inetAddress.hostAddress }
        }
        return localAddresses.toList()
    }
}

class GatewayIPAddressException(message: String) : Exception(message)
