package tech.relaycorp.courier.data.model

import kotlin.math.min

data class StorageSize(
    val bytes: Long
) {
    operator fun plus(size: StorageSize) = StorageSize(bytes + size.bytes)
    operator fun minus(size: StorageSize) = StorageSize(bytes - size.bytes)

    companion object {
        fun min(size1: StorageSize, size2: StorageSize) =
            StorageSize(min(size1.bytes, size2.bytes))
    }
}
