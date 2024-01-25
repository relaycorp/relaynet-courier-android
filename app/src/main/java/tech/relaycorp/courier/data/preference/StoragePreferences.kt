package tech.relaycorp.courier.data.preference

import androidx.annotation.VisibleForTesting
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.map
import tech.relaycorp.courier.data.model.StorageSize
import javax.inject.Inject
import javax.inject.Provider

class StoragePreferences
    @Inject
    constructor(
        private val preferences: Provider<FlowSharedPreferences>,
    ) {
        private val maxStorage by lazy {
            preferences.get().getLong("max_storage", DEFAULT_MAX_STORAGE_SIZE.bytes)
        }

        fun getMaxStorageSize() = { maxStorage }.toFlow().map { StorageSize(it) }

        suspend fun setMaxStorageSize(value: StorageSize) = maxStorage.setAndCommit(value.bytes)

        companion object {
            @VisibleForTesting
            internal val DEFAULT_MAX_STORAGE_SIZE = StorageSize(1_000_000_000)
        }
    }
