package tech.relaycorp.courier.ui.settings

import tech.relaycorp.courier.data.model.StorageSize

data class StorageStats(
    val used: StorageSize,
    val usedPercentage: Int,
    val available: StorageSize,
    val total: StorageSize
)
