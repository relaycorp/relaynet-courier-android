package tech.relaycorp.courier.domain

import kotlinx.coroutines.flow.combine
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskStats
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.preference.StoragePreferences
import javax.inject.Inject
import kotlin.math.min

class ObserveStorageUsage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val storagePreferences: StoragePreferences,
    private val diskStats: DiskStats
) {

    fun observe() =
        combine(
            storedMessageDao.observeFullSize(),
            observeMaxStorageAvailable()
        ) { usedStorage, maxStorageAvailable ->
            StorageUsage(usedStorage, maxStorageAvailable)
        }

    private fun observeMaxStorageAvailable() =
        combine(
            storagePreferences.getMaxStoragePercentage(),
            diskStats.observeAvailableStorage(),
            storedMessageDao.observeFullSize()
        ) { maxStoragePercentage, availableStorage, usedStorage ->
            val totalStorage = diskStats.totalStorage
            min(
                (maxStoragePercentage / 100.0 * totalStorage).toLong(),
                availableStorage + usedStorage
            )
        }
}
