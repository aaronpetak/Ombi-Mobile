package com.ombi.mobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the JWT auth token for the current Ombi session.
 *
 * The token is normally stored in [EncryptedSharedPreferences] backed by an
 * AES-256-GCM key held in the Android Keystore, so it is encrypted at rest and
 * tied to the device.
 *
 * The Keystore can fail to initialise on some devices (corrupted key material,
 * OEM Keystore bugs, or a partially-written prefs file after a crash). Because
 * this class is a `@Singleton` injected into the network stack and both
 * repositories, an exception in a property initialiser would take down the
 * entire Hilt object graph at startup. To avoid that, initialisation is wrapped
 * in [buildPrefs]: on failure the (possibly corrupt) prefs file is deleted and
 * creation is retried once; if it still fails we fall back to a cleared,
 * unencrypted [SharedPreferences] and flag [secureStorageAvailable] as false so
 * the UI can warn the user. The app stays usable rather than crash-looping.
 *
 * All reads and writes are synchronous and safe to call from any thread.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG          = "AuthManager"
        private const val PREFS_NAME   = "ombi_auth"
        private const val FALLBACK_PREFS_NAME = "ombi_auth_fallback"
        private const val KEY_TOKEN    = "auth_token"
        private const val KEY_EXPIRY   = "token_expiry"
        private const val KEY_USERNAME = "username"
    }

    /**
     * True when the token is stored in Keystore-backed [EncryptedSharedPreferences].
     * False when secure storage could not be initialised and an unencrypted
     * fallback is in use — surface a warning to the user in that case.
     */
    @Volatile
    var secureStorageAvailable: Boolean = true
        private set

    private val prefs: SharedPreferences = buildPrefs()

    /**
     * Builds the credential store, degrading gracefully if the Keystore-backed
     * store cannot be created.
     *
     * Order of attempts:
     * 1. Create [EncryptedSharedPreferences] normally.
     * 2. On failure, delete the (possibly corrupt) prefs file and retry once —
     *    this recovers from a prefs file left half-written by a crash mid-commit.
     * 3. If it still fails, return a freshly-cleared unencrypted store and mark
     *    [secureStorageAvailable] = false.
     */
    private fun buildPrefs(): SharedPreferences {
        try {
            return createEncryptedPrefs()
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences init failed; clearing and retrying", e)
        }

        // The prefs file (and its Keystore key) may be corrupt. Delete both and
        // retry — the user is logged out, but the app recovers cleanly.
        deleteEncryptedPrefsFile()
        try {
            return createEncryptedPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable; falling back to plaintext", e)
        }

        secureStorageAvailable = false
        // Start the fallback store empty so no stale token lingers unencrypted.
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            .also { it.edit { clear() } }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Deletes the encrypted prefs file so a fresh one can be created. */
    private fun deleteEncryptedPrefsFile() {
        try {
            context.deleteSharedPreferences(PREFS_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete corrupt prefs file", e)
        }
    }

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
