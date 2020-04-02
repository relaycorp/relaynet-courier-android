package tech.relaycorp.courier.ui.settings

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.domain.DeleteAllStorage
import tech.relaycorp.courier.domain.ObserveStorageUsage
import tech.relaycorp.courier.test.factory.StorageUsageFactory
import tech.relaycorp.courier.ui.common.EnableState

internal class SettingsViewModelTest {

    private val observeStorageUsage = mock<ObserveStorageUsage>()
    private val deleteAllStorage = mock<DeleteAllStorage>()

    @BeforeEach
    internal fun setUp() {
        whenever(observeStorageUsage.observe()).thenReturn(emptyFlow())
    }

    @Test
    fun deleteDataClicked() = runBlockingTest {
        buildViewModel().deleteDataClicked()
        verify(deleteAllStorage).delete()
    }

    @Test
    fun deleteDataEnabled_emptyData() = runBlockingTest {
        val emptyUsage = StorageUsageFactory.build().copy(usedByApp = StorageSize(0L))
        whenever(observeStorageUsage.observe()).thenReturn(flowOf(emptyUsage))

        val vm = buildViewModel()
        assertEquals(
            EnableState.Disabled,
            vm.deleteDataEnabled().first()
        )
    }

    @Test
    fun deleteDataEnabled_someData() = runBlockingTest {
        val emptyUsage = StorageUsageFactory.build().copy(usedByApp = StorageSize(1L))
        whenever(observeStorageUsage.observe()).thenReturn(flowOf(emptyUsage))

        val vm = buildViewModel()
        assertEquals(
            EnableState.Enabled,
            vm.deleteDataEnabled().first()
        )
    }

    private fun buildViewModel() = SettingsViewModel(observeStorageUsage, deleteAllStorage)
}
