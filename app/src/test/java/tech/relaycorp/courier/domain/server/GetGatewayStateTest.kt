package tech.relaycorp.courier.domain.server

import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.cogrpc.server.Networking

internal class GetGatewayStateTest {

    private val spiedNetworking = spy(Networking)
    private val localIPAddress = "192.168.43.1"

    @Test
    fun testGatewayStateAvailable() {
        // Arrange
        whenever(spiedNetworking.getAllLocalIpAddresses()).thenReturn(listOf(localIPAddress))
        val getState = build()

        // Act
        val result = getState.invoke()

        // Assert
        assertEquals(GetGatewayState.GatewayState.Available, result)
    }

    @Test
    fun testGatewayStateUnavailable() {
        // Arrange
        whenever(spiedNetworking.getAllLocalIpAddresses()).thenReturn(emptyList())
        val getState = build()

        // Act
        val result = getState.invoke()

        // Assert
        assertEquals(GetGatewayState.GatewayState.Unavailable, result)
    }

    fun build(): GetGatewayState = GetGatewayState(spiedNetworking::getGatewayIpAddress)
}
