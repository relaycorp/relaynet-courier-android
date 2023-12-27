package tech.relaycorp.courier.data.disk

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import tech.relaycorp.courier.data.model.StorageSize
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class DiskStats
    @Inject
    constructor() {
        private val internalStats by lazy { StatFs(Environment.getDataDirectory().path) }

        suspend fun getTotalStorage() = withContext(Dispatchers.IO) { StorageSize(internalStats.totalBytes) }

        suspend fun getAvailableStorage() = withContext(Dispatchers.IO) { StorageSize(internalStats.availableBytes) }

        fun observeAvailableStorage() =
            flow {
                while (true) {
                    emit(getAvailableStorage())
                    delay(10.seconds)
                }
            }.distinctUntilChanged()
    }
