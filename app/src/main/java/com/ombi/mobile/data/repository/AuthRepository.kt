package com.ombi.mobile.data.repository

import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.api.models.UserAuthRequest
import com.ombi.mobile.data.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: OmbiApiService,
    private val authManager: AuthManager
) {
    val username: String? get() = authManager.getUsername()

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

    fun logout() = authManager.clearToken()
}
