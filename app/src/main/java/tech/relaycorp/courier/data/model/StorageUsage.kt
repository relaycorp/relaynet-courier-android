package tech.relaycorp.courier.data.model

import androidx.annotation.VisibleForTesting
import kotlin.math.roundToInt

data class StorageUsage(
    val usedByApp: StorageSize,
    val definedMax: StorageSize,
    val actualMax: StorageSize = definedMax
) {
    private val ratio get() = usedByApp.bytes.toFloat() / actualMax.bytes
    val percentage get() = (ratio * 100).roundToInt()

    val available get() = (actualMax - usedByApp).coerceAtLeast(StorageSize.ZERO)
    val isLowOnSpace get() = available < LOW_THRESHOLD

    companion object {
        @VisibleForTesting
        val LOW_THRESHOLD = StorageSize(50_000_000)
    }
}
