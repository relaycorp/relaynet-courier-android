package tech.relaycorp.courier.domain

import kotlinx.coroutines.flow.combine
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskStats
import tech.relaycorp.courier.data.model.StorageSize.Companion.min
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.preference.StoragePreferences
import javax.inject.Inject

class ObserveStorageUsage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val storagePreferences: StoragePreferences,
    private val diskStats: DiskStats
) {

    fun observe() =
        combine(
            storedMessageDao.observeTotalSize(),
            storagePreferences.getMaxStorageSize(),
            observeActualMax()
        ) { usedByApp, definedMax, actualMax ->
            StorageUsage(usedByApp, definedMax, actualMax)
        }

    // If there's less than the user-defined max storage available,
    // return the actually available storage size
    private fun observeActualMax() =
        combine(
            storagePreferences.getMaxStorageSize(),
            storedMessageDao.observeTotalSize(),
            diskStats.observeAvailableStorage()
        ) { definedMax, usedByApp, available ->
            min(definedMax, usedByApp + available)
        }
}
