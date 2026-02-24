package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserViewModel(
    @Json(name = "id") val id: String,
    @Json(name = "userName") val userName: String,
    @Json(name = "alias") val alias: String?,
    @Json(name = "emailAddress") val emailAddress: String?,
    // 1 = LocalUser, 2 = PlexUser, 3 = EmbyUser
    @Json(name = "userType") val userType: Int,
    @Json(name = "hasLoggedIn") val hasLoggedIn: Boolean
)
