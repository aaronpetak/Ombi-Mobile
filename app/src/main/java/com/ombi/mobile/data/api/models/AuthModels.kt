package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserAuthRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "rememberMe") val rememberMe: Boolean = true,
    @Json(name = "usePlexOAuth") val usePlexOAuth: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PlexTokenAuthRequest(
    @Json(name = "token") val token: String
)

@JsonClass(generateAdapter = true)
data class AuthTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expiration") val expiration: String
)
