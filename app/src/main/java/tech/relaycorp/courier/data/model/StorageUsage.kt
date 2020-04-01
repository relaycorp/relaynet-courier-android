package tech.relaycorp.courier.data.model

import kotlin.math.roundToInt

data class StorageUsage(
    val usedByApp: StorageSize,
    val definedMax: StorageSize,
    val actualMax: StorageSize = definedMax
) {
    private val ratio get() = usedByApp.bytes.toFloat() / actualMax.bytes
    val percentage get() = (ratio * 100).roundToInt()
}
