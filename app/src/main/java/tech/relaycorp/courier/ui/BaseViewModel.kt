package tech.relaycorp.courier.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus

abstract class BaseViewModel : ViewModel() {
    val ioScope = viewModelScope + Dispatchers.IO
}
