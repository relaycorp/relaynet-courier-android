package tech.relaycorp.courier.test.factory

import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import java.util.Random

object StorageUsageFactory {
    fun build(): StorageUsage {
        val used = Random().nextInt(1_000).toLong()
        return StorageUsage(
            StorageSize(used),
            StorageSize(used + Random().nextInt(1_000_000).toLong()),
        )
    }
}
