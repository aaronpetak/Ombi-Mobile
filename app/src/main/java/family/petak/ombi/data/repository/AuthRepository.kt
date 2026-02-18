package family.petak.ombi.data.repository

import family.petak.ombi.data.api.OmbiApiService
import family.petak.ombi.data.api.models.PlexTokenAuthRequest
import family.petak.ombi.data.api.models.UserAuthRequest
import family.petak.ombi.data.auth.AuthManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: OmbiApiService,
    private val authManager: AuthManager
) {
    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
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

    suspend fun loginWithPlexToken(plexToken: String): Result<Unit> {
        return try {
            val response = api.loginWithPlexToken(PlexTokenAuthRequest(plexToken))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                authManager.saveToken(token.accessToken, token.expiration, "plex_user")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Plex login failed (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = authManager.clearToken()
}
