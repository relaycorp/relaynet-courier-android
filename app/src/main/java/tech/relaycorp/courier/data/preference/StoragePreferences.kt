package tech.relaycorp.courier.data.preference

import androidx.annotation.VisibleForTesting
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.asFlow
import javax.inject.Inject
import javax.inject.Provider

class StoragePreferences
@Inject constructor(
    private val preferences: Provider<FlowSharedPreferences>
) {

    private val maxStorage by lazy {
        preferences.get().getInt("max_storage", DEFAULT_MAX_STORAGE_PERCENTAGE)
    }

    fun getMaxStoragePercentage() = { maxStorage }.toFlow()
    suspend fun setMaxStoragePercentage(value: Int) = maxStorage.setAndCommit(value)

    fun a() = { 1 }.asFlow()

    companion object {
        @VisibleForTesting
        internal const val DEFAULT_MAX_STORAGE_PERCENTAGE = 10
    }
}
