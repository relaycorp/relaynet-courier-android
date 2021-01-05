package tech.relaycorp.cogrpc.server

import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NetworkingTest {
    @Nested
    inner class GetGatewayIpAddress {
        private val spiedNetworking = spy(Networking)
        private val localIPAddress = "192.168.43.1"

        @Test
        fun `An exception should be thrown if no IP address could be found`() {
            whenever(spiedNetworking.getAllLocalIpAddresses()).thenReturn(emptyList())

            val exception =
                assertThrows<GatewayIPAddressException> { spiedNetworking.getGatewayIpAddress() }

            assertEquals(
                "No address with the prefix 192.168. was found",
                exception.message
            )
        }

        @Test
        fun `An exception should be thrown if multiple IP addresses are found`() {
            whenever(spiedNetworking.getAllLocalIpAddresses()).thenReturn(
                listOf(localIPAddress, "192.168.43.200")
            )

            val exception =
                assertThrows<GatewayIPAddressException> { spiedNetworking.getGatewayIpAddress() }

            assertEquals(
                "Multiple addresses with the prefix 192.168. were found",
                exception.message
            )
        }

        @Test
        fun `Addresses that do not start with the Android gateway prefix should be ignored`() {
            whenever(spiedNetworking.getAllLocalIpAddresses()).thenReturn(
                listOf("10.0.0.1", localIPAddress)
            )

            assertEquals(spiedNetworking.getGatewayIpAddress(), localIPAddress)
        }

        @Test
        fun `The IP address should be returned if there is only one address`() {
            whenever(spiedNetworking.getAllLocalIpAddresses()).thenReturn(listOf(localIPAddress))

            assertEquals(spiedNetworking.getGatewayIpAddress(), localIPAddress)
        }
    }
}
