package com.ombi.mobile.data.repository

import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.api.models.UserAuthRequest
import com.ombi.mobile.data.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles authentication operations against the Ombi API.
 *
 * Acts as the single source of truth for auth state — all login and logout
 * actions flow through here. On a successful login the JWT token is handed
 * to [AuthManager] for encrypted, persistent storage.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: OmbiApiService,
    private val authManager: AuthManager
) {
    /** The username stored from the most recent successful login, or null if signed out. */
    val username: String? get() = authManager.getUsername()

    /**
     * Authenticates with an Ombi local account (username + password).
     *
     * A blank password is allowed — some Ombi installations have passwordless
     * accounts. Only [username] is required.
     *
     * @return [Result.success] on a valid response; [Result.failure] with a
     *         descriptive message on HTTP errors or network failures.
     */
    suspend fun loginWithCredentials(username: String, password: String): Result<Unit> {
        return try {
            val response = api.login(UserAuthRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                authManager.saveToken(token.accessToken, token.expiration, username)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid credentials (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears the stored token, ending the current session.
     * After this call the user will be redirected to the login screen.
     */
    fun logout() = authManager.clearToken()
}
