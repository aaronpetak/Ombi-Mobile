package com.ombi.mobile.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the JWT auth token for the current Ombi session.
 *
 * The token is stored in [EncryptedSharedPreferences] backed by an AES-256-GCM
 * key held in the Android Keystore. This means the token is encrypted at rest
 * and tied to the device — it cannot be extracted by backing up the app data.
 *
 * All reads and writes are synchronous and safe to call from any thread.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME  = "ombi_auth"
        private const val KEY_TOKEN   = "auth_token"
        private const val KEY_EXPIRY  = "token_expiry"
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

    /** Returns the stored Bearer token, or null if the user is not signed in. */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** Returns the username associated with the current session, or null if not signed in. */
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    /**
     * Persists a new auth token after a successful login.
     *
     * @param token      The JWT Bearer token returned by the Ombi API.
     * @param expiry     The token expiration timestamp string returned by the API.
     * @param username   The username to associate with this session.
     */
    fun saveToken(token: String, expiry: String, username: String) {
        prefs.edit {
            putString(KEY_TOKEN, token)
            putString(KEY_EXPIRY, expiry)
            putString(KEY_USERNAME, username)
        }
    }

    /**
     * Clears all stored credentials, effectively signing the user out.
     * After this call [getToken] and [getUsername] will return null.
     */
    fun clearToken() {
        prefs.edit { clear() }
    }
}
