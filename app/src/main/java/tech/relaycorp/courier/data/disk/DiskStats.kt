package tech.relaycorp.courier.data.disk

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import tech.relaycorp.courier.data.model.StorageSize
import javax.inject.Inject
import kotlin.time.seconds

class DiskStats
@Inject constructor() {

    private val internalStats by lazy { StatFs(Environment.getDataDirectory().path) }

    val totalStorage
        get() =
            StorageSize(internalStats.totalBytes)

    val availableStorage
        get() =
            StorageSize(internalStats.availableBytes)

    fun observeAvailableStorage() =
        flow {
            while (true) {
                emit(availableStorage)
                delay(10.seconds)
            }
        }.distinctUntilChanged()
}
