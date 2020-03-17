package tech.relaycorp.courier.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    fun io(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(Dispatchers.IO, block = block)
}
