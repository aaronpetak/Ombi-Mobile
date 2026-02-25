package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a single permission claim from the Ombi identity endpoint.
 *
 * Ombi models all user permissions as a flat list of named claims, each with
 * a boolean [enabled] flag. The claim name is stored in [value]
 * (e.g. "Admin", "RequestMovie", "PowerUser").
 *
 * Returned as part of [UserViewModel.claims] from GET /api/v1/identity.
 */
@JsonClass(generateAdapter = true)
data class ClaimCheckbox(
    // "value" is the claim name (e.g. "Admin"), "enabled" is whether the user holds it
    @Json(name = "value")   val value: String,
    @Json(name = "enabled") val enabled: Boolean
)

/**
 * The currently authenticated user, returned by GET /api/v1/identity.
 *
 * [userType] indicates the account origin:
 *  - 1 = Local Ombi account
 *  - 2 = Plex account
 *  - 3 = Emby/Jellyfin account
 *
 * [claims] contains the full set of Ombi permission claims for this user.
 * Use [isAdmin] for a quick admin check rather than inspecting [claims] directly.
 */
@JsonClass(generateAdapter = true)
data class UserViewModel(
    @Json(name = "id")           val id: String,
    @Json(name = "userName")     val userName: String,
    @Json(name = "alias")        val alias: String?,
    @Json(name = "emailAddress") val emailAddress: String?,
    // 1 = LocalUser, 2 = PlexUser, 3 = EmbyUser
    @Json(name = "userType")     val userType: Int,
    @Json(name = "hasLoggedIn")  val hasLoggedIn: Boolean,
    @Json(name = "claims")       val claims: List<ClaimCheckbox>?
) {
    /** True if the user holds the "Admin" claim with [ClaimCheckbox.enabled] = true. */
    val isAdmin: Boolean get() = claims?.any { it.value == "Admin" && it.enabled } == true
}
