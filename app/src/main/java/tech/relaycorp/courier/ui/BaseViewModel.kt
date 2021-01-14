package tech.relaycorp.courier.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus

abstract class BaseViewModel : ViewModel() {
    val scope = viewModelScope + VIEW_MODEL_DISPATCHER

    companion object {
        @VisibleForTesting
        var VIEW_MODEL_DISPATCHER = Dispatchers.IO
    }
}
