package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for POST /api/v1/token (username + password login).
 *
 * [rememberMe] keeps the session alive across app restarts; always true for
 * a mobile client. [usePlexOAuth] must be false for standard Ombi local
 * account login.
 */
@JsonClass(generateAdapter = true)
data class UserAuthRequest(
    @Json(name = "username")    val username: String,
    @Json(name = "password")    val password: String,
    @Json(name = "rememberMe")  val rememberMe: Boolean = true,
    @Json(name = "usePlexOAuth") val usePlexOAuth: Boolean = false
)

/**
 * Request body for POST /api/v1/token/plextoken (Plex SSO login).
 *
 * The caller must obtain [token] from the Plex OAuth flow before calling
 * this endpoint.
 */
@JsonClass(generateAdapter = true)
data class PlexTokenAuthRequest(
    @Json(name = "token") val token: String
)

/**
 * Successful response body from both auth endpoints.
 *
 * [accessToken] is a JWT that must be included as a Bearer token on every
 * subsequent API request. [expiration] is an ISO-8601 timestamp indicating
 * when the token becomes invalid.
 */
@JsonClass(generateAdapter = true)
data class AuthTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expiration")   val expiration: String
)
