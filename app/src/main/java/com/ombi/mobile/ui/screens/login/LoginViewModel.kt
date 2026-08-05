package com.ombi.mobile.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Login screen. */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the Login screen.
 *
 * Validates the username field (required) and delegates credential
 * authentication to [AuthRepository]. The password field is optional —
 * some Ombi accounts are configured without a password, in which case an
 * empty string is passed to the API and Ombi accepts it.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Updates the username field and clears any previous error. */
    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    /** Updates the password field and clears any previous error. */
    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    /**
     * Attempts login with the current username/password.
     * Validates that username is non-blank, then calls the Ombi token endpoint.
     * On success, invokes [onSuccess] (which navigates to the main screen).
     * On failure, the error message is surfaced in [LoginUiState.error].
     */
    fun loginWithCredentials(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.value = state.copy(error = "Username is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            authRepository.loginWithCredentials(state.username, state.password)
                .onSuccess {
                    // Reset isLoading before navigating so that if onSuccess() throws
                    // (e.g. NavController already destroyed after a config change) the
                    // login button is not left permanently disabled. Also clear the
                    // password so it does not linger in the retained StateFlow.
                    _uiState.value = _uiState.value.copy(isLoading = false, password = "")
                    onSuccess()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

}
