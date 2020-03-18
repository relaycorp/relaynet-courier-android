package tech.relaycorp.courier.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver

internal class MainViewModelTest {

    private val connectionObserver = mock<InternetConnectionObserver>()

    @Test
    internal fun syncMode() = runBlockingTest {
        val states = ConflatedBroadcastChannel<InternetConnection>()
        whenever(connectionObserver.observe()).thenReturn(states.asFlow())
        val viewModel = buildViewModel()

        states.send(InternetConnection.Offline)
        viewModel.syncMode.take(1).collect {
            assertEquals(MainViewModel.SyncMode.People, it)
        }

        states.send(InternetConnection.Online)
        viewModel.syncMode.take(1).collect {
            assertEquals(MainViewModel.SyncMode.Internet, it)
        }
    }

    private fun buildViewModel() = MainViewModel(connectionObserver)
}
