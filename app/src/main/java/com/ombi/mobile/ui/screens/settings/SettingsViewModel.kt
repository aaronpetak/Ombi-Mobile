package com.ombi.mobile.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.preferences.UserPreferences
import com.ombi.mobile.data.repository.AuthRepository
import com.ombi.mobile.data.repository.OmbiRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Settings screen.
 *
 * @param serverUrl The currently configured Ombi server URL.
 * @param username The logged-in username retrieved from [AuthManager].
 * @param theme Active theme preference: "system", "dark", or "light".
 * @param isLoading Reserved for future async operations.
 */
data class SettingsUiState(
    val serverUrl: String = "",
    val username: String? = null,
    val theme: String = "system",
    val isLoading: Boolean = false
)

/**
 * ViewModel for the Settings screen.
 *
 * [uiState] is derived reactively by combining the [serverUrl] and [theme] DataStore
 * flows. The username is read synchronously from [AuthRepository] (via [AuthManager])
 * since it was already stored at login time and does not change during a session.
 *
 * Theme changes are persisted immediately via [UserPreferences.setTheme] and observed
 * in [MainActivity] to apply the correct [OmbiTheme] variant without restarting the app.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val ombiRepository: OmbiRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferences.serverUrl,
        userPreferences.theme
    ) { url, theme ->
        SettingsUiState(
            serverUrl = url,
            username = authRepository.username,
            theme = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        // Load display name from API
        viewModelScope.launch {
            ombiRepository.getCurrentUser().onSuccess { user ->
                // username is already stored in AuthManager from login; no additional update needed
                // If we want to show display name, it's available as user.displayName
            }
        }
    }

    /** Persists the selected theme ("system", "dark", or "light") to DataStore. */
    fun setTheme(theme: String) {
        viewModelScope.launch { userPreferences.setTheme(theme) }
    }

    /** Clears the stored token and username, then invokes [onLogout] to navigate to Login. */
    fun logout(onLogout: () -> Unit) {
        authRepository.logout()
        onLogout()
    }
}
