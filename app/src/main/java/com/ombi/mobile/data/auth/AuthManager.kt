package com.ombi.mobile.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the JWT auth token, stored in EncryptedSharedPreferences backed by
 * the Android Keystore (AES256-GCM key).
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "ombi_auth"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EXPIRY = "token_expiry"
        private const val KEY_USERNAME = "username"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun saveToken(token: String, expiry: String, username: String) {
        prefs.edit {
            putString(KEY_TOKEN, token)
            putString(KEY_EXPIRY, expiry)
            putString(KEY_USERNAME, username)
        }
    }

    fun clearToken() {
        prefs.edit { clear() }
    }
}
