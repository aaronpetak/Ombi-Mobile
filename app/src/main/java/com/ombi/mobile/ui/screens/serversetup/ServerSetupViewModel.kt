package com.ombi.mobile.ui.screens.serversetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerSetupUiState(
    val url: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerSetupUiState())
    val uiState: StateFlow<ServerSetupUiState> = _uiState.asStateFlow()

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null)
    }

    fun save(onSuccess: () -> Unit) {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Server URL cannot be empty")
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _uiState.value = _uiState.value.copy(error = "URL must start with http:// or https://")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            userPreferences.setServerUrl(url)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}
