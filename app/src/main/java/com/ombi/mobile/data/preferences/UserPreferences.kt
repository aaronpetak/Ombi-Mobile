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

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_THEME = stringPreferencesKey("theme")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map {
        it[KEY_SERVER_URL] ?: ""
    }

    val theme: Flow<String> = context.dataStore.data.map {
        it[KEY_THEME] ?: "dark"
    }

    /**
     * Synchronous read for use in the OkHttp interceptor (runs on a background thread).
     */
    fun getServerUrlSync(): String = runBlocking { serverUrl.first() }

    suspend fun setServerUrl(url: String) {
        val normalized = url.trimEnd('/')
        context.dataStore.edit { it[KEY_SERVER_URL] = normalized }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }
}
