package tech.relaycorp.courier.background

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.AppModule
import tech.relaycorp.courier.domain.server.GetGatewayState
import tech.relaycorp.courier.domain.server.GetGatewayState.GatewayState

class WifiHotspotStateWatcherTest {
    private val context = mock<Context>()
    private val foregroundAppMonitor = mock<ForegroundAppMonitor>()
    private val getGatewayIpAddress = mock<GetGatewayState>()
    private val testCoroutineScope = TestCoroutineScope()

    private val wifiHotspotStateWatcher = WifiHotspotStateWatcher(
        context,
        AppModule.WifiApStateAvailability.Unavailable,
        foregroundAppMonitor,
        getGatewayIpAddress,
        testCoroutineScope.coroutineContext
    )

    @Test
    fun backgroundPollingCheck() = runBlockingTest(testCoroutineScope.coroutineContext) {
        whenever(foregroundAppMonitor.observe()).thenReturn(flowOf(ForegroundAppMonitor.State.Background))
        wifiHotspotStateWatcher.start()

        verify(getGatewayIpAddress, never()).invoke()
    }

    @Test
    fun foregroundPollingCheck() = runBlockingTest(testCoroutineScope.coroutineContext) {
        whenever(foregroundAppMonitor.observe()).thenReturn(flowOf(ForegroundAppMonitor.State.Foreground))
        whenever(getGatewayIpAddress.invoke()).thenReturn(GatewayState.Available)
        wifiHotspotStateWatcher.start()

        verify(getGatewayIpAddress).invoke()
        wifiHotspotStateWatcher.stop()
    }

    @Test
    fun hotspotDisabledCheck() = runBlockingTest(testCoroutineScope.coroutineContext) {
        whenever(foregroundAppMonitor.observe()).thenReturn(flowOf(ForegroundAppMonitor.State.Foreground))
        whenever(getGatewayIpAddress.invoke()).thenReturn(GatewayState.Unavailable)
        wifiHotspotStateWatcher.start()
        val hotspotState = wifiHotspotStateWatcher.state().first()

        assertEquals(WifiHotspotState.Disabled, hotspotState)
        wifiHotspotStateWatcher.stop()
    }

    @Test
    fun hotspotEnabledCheck() = runBlockingTest(testCoroutineScope.coroutineContext) {
        whenever(foregroundAppMonitor.observe()).thenReturn(flowOf(ForegroundAppMonitor.State.Foreground))
        whenever(getGatewayIpAddress.invoke()).thenReturn(GatewayState.Available)
        wifiHotspotStateWatcher.start()
        val hotspotState = wifiHotspotStateWatcher.state().first()

        assertEquals(WifiHotspotState.Enabled, hotspotState)
        wifiHotspotStateWatcher.stop()
    }
}
