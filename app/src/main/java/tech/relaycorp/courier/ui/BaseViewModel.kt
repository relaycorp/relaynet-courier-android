package tech.relaycorp.courier.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus

abstract class BaseViewModel : ViewModel() {
    val scope = viewModelScope + viewModelDispatcher

    companion object {
        @VisibleForTesting
        var viewModelDispatcher = Dispatchers.IO
    }
}
