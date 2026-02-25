package com.ombi.mobile.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore instance scoped to the application context. */
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

/**
 * Persists user-configurable preferences using Jetpack DataStore.
 *
 * Stored values:
 * - **serverUrl** — The base URL of the Ombi backend the user has configured
 *   (e.g. `https://ombi.example.com`). All API requests are routed to this URL
 *   at runtime by the [com.ombi.mobile.di.NetworkModule] dynamic URL interceptor.
 * - **theme** — The UI theme preference: `"dark"`, `"light"`, or `"system"`.
 *
 * Preferences are stored as plain strings in a DataStore Preferences file.
 * Unlike [com.ombi.mobile.data.auth.AuthManager], this data is not encrypted
 * because it contains no secrets.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_THEME      = stringPreferencesKey("theme")
    }

    /** Reactive stream of the configured Ombi server URL. Emits "" if not yet set. */
    val serverUrl: Flow<String> = context.dataStore.data.map {
        it[KEY_SERVER_URL] ?: ""
    }

    /** Reactive stream of the theme preference. Defaults to "dark" if not yet set. */
    val theme: Flow<String> = context.dataStore.data.map {
        it[KEY_THEME] ?: "dark"
    }

    /**
     * Synchronous read of the server URL for use inside OkHttp interceptors,
     * which run on background threads outside of coroutine scope.
     *
     * Safe to call from any thread — [runBlocking] is acceptable here because
     * the OkHttp dispatcher runs on a dedicated background thread pool.
     */
    fun getServerUrlSync(): String = runBlocking { serverUrl.first() }

    /**
     * Persists a new server URL, trimming any trailing slash to ensure consistent
     * path concatenation with Retrofit.
     */
    suspend fun setServerUrl(url: String) {
        val normalized = url.trimEnd('/')
        context.dataStore.edit { it[KEY_SERVER_URL] = normalized }
    }

    /** Persists the user's theme preference ("dark", "light", or "system"). */
    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }
}
