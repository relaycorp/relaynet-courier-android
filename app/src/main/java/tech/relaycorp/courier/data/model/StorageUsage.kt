package tech.relaycorp.courier.data.model

import kotlin.math.roundToInt

data class StorageUsage(
    val used: Long,
    val totalAvailable: Long
) {
    val percentage get() = ((used.toFloat() / totalAvailable) * 100).roundToInt()
}
