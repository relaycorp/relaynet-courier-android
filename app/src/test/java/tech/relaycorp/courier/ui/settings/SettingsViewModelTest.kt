package tech.relaycorp.courier.ui.settings

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.disk.DiskStats
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.preference.StoragePreferences
import tech.relaycorp.courier.domain.DeleteAllStorage
import tech.relaycorp.courier.domain.GetStorageUsage
import tech.relaycorp.courier.test.WaitAssertions.waitForAssertEquals
import tech.relaycorp.courier.test.factory.StorageUsageFactory
import tech.relaycorp.courier.ui.common.EnableState

internal class SettingsViewModelTest {

    private val observeStorageUsage = mock<GetStorageUsage>()
    private val deleteAllStorage = mock<DeleteAllStorage>()
    private val storagePreferences = mock<StoragePreferences>()
    private val diskStats = mock<DiskStats>() {
        onBlocking { getTotalStorage() }.thenReturn(StorageSize.ZERO)
    }

    @BeforeEach
    internal fun setUp() {
        whenever(observeStorageUsage.observe()).thenReturn(emptyFlow())
        whenever(storagePreferences.getMaxStorageSize()).thenReturn(emptyFlow())
        whenever(diskStats.observeAvailableStorage()).thenReturn(emptyFlow())
    }

    @Test
    @Ignore("Failing intermittently") // TODO: fix test
    fun `deleteData clicked called right domain method`() = runBlocking(Dispatchers.IO) {
        buildViewModel().deleteDataClicked()
        verify(deleteAllStorage).delete()
    }

    @Test
    fun `deleteData is disabled when data is empty`() = runBlocking(Dispatchers.IO) {
        val emptyUsage = StorageUsageFactory.build().copy(usedByApp = StorageSize(0L))
        whenever(observeStorageUsage.observe()).thenReturn(flowOf(emptyUsage))

        val vm = buildViewModel()
        waitForAssertEquals(
            EnableState.Disabled,
            vm.deleteDataEnabled()::first
        )
    }

    @Test
    fun `deleteData is enabled when there's data`() = runBlocking(Dispatchers.IO) {
        val emptyUsage = StorageUsageFactory.build().copy(usedByApp = StorageSize(1L))
        whenever(observeStorageUsage.observe()).thenReturn(flowOf(emptyUsage))

        val vm = buildViewModel()
        waitForAssertEquals(
            EnableState.Enabled,
            vm.deleteDataEnabled()::first
        )
    }

    @Test
    fun `observe storage stats`() = runBlocking(Dispatchers.IO) {
        val total = StorageSize(1_000_000)
        val available = StorageSize(500_000)
        val used = StorageSize(100_000)
        val usage = StorageUsage(used, total, available)
        whenever(observeStorageUsage.observe()).thenReturn(flowOf(usage))
        whenever(diskStats.getTotalStorage()).thenReturn(total)
        whenever(diskStats.observeAvailableStorage()).thenReturn(flowOf(available))

        val vm = buildViewModel()
        waitForAssertEquals(
            StorageStats(used, 20, available, total),
            vm.storageStats()::first
        )
    }

    @Test
    fun `max storage boundary`() = runBlocking(Dispatchers.IO) {
        val totalStorage = SettingsViewModel.MIN_STORAGE_SIZE * 10
        whenever(diskStats.getTotalStorage()).thenReturn(totalStorage)

        val vm = buildViewModel()
        waitForAssertEquals(
            SizeBoundary(
                SettingsViewModel.MIN_STORAGE_SIZE,
                totalStorage,
                SettingsViewModel.STORAGE_SIZE_STEP
            ),
            vm.maxStorageBoundary()::first
        )
    }

    @Test
    internal fun `max storage value changed`() = runBlocking {
        val newValue = StorageSize(1_000_000_00)
        runBlocking(Dispatchers.IO) {
            buildViewModel().maxStorageChanged(newValue)
        }
        verify(storagePreferences).setMaxStorageSize(eq(newValue))
    }

    private fun buildViewModel() =
        SettingsViewModel(observeStorageUsage, deleteAllStorage, storagePreferences, diskStats)
}
