package tech.relaycorp.courier.ui.settings

import tech.relaycorp.courier.data.model.StorageSize

data class SizeBoundary(
    val min: StorageSize,
    val max: StorageSize,
    val step: StorageSize
) {
    val steppedMax get() = max - (max % step)
    val steppedMin get() = min + (min % step)
}
