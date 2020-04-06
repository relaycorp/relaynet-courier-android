package tech.relaycorp.courier.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.domain.DeleteExpiredMessages
import tech.relaycorp.courier.domain.ObserveStorageUsage
import tech.relaycorp.courier.test.factory.StorageUsageFactory
import tech.relaycorp.courier.test.test

internal class MainViewModelTest {

    private val connectionObserver = mock<InternetConnectionObserver>()
    private val observeStorageUsage = mock<ObserveStorageUsage>()
    private val deleteExpiredMessages = mock<DeleteExpiredMessages>()

    @BeforeEach
    internal fun setUp() {
        whenever(connectionObserver.observe()).thenReturn(emptyFlow())
        whenever(observeStorageUsage.observe()).thenReturn(emptyFlow())
    }

    @Test
    internal fun syncMode() = runBlockingTest {
        val states = ConflatedBroadcastChannel<InternetConnection>()
        whenever(connectionObserver.observe()).thenReturn(states.asFlow())
        val viewModel = buildViewModel()
        val syncMode = viewModel.syncMode().test(this)

        states.send(InternetConnection.Offline)
        states.send(InternetConnection.Online)

        syncMode
            .assertValues(MainViewModel.SyncMode.People, MainViewModel.SyncMode.Internet)
            .finish()
    }

    @Test
    internal fun storageUsage() = runBlockingTest {
        val storageUsage = StorageUsageFactory.build()
        whenever(observeStorageUsage.observe()).thenReturn(flowOf(storageUsage))

        val viewModel = buildViewModel()
        assertEquals(storageUsage, viewModel.storageUsage().first())
    }

    @Test
    internal fun `deletes expired messages`() = runBlockingTest {
        buildViewModel()
        verify(deleteExpiredMessages).delete()
    }

    private fun buildViewModel() =
        MainViewModel(connectionObserver, observeStorageUsage, deleteExpiredMessages)
}
