package tech.relaycorp.courier.test.factory

import tech.relaycorp.courier.data.model.StorageSize
import java.util.Random

object StorageSizeFactory {
    fun build() = StorageSize(Random().nextInt(1000).toLong())
}
