package com.ombi.mobile.data.auth

/**
 * A reason the current session ended. Emitted by [AuthManager.sessionEvents]
 * so the UI can clear navigation back to the login screen from a single place.
 */
enum class SessionEvent {
    /** The user explicitly signed out. */
    LOGGED_OUT,

    /**
     * The server rejected an authenticated request with 401, meaning the token
     * has expired or been revoked. Covers both client-side expiry and
     * server-side revocation — no client-side expiry check is needed.
     */
    EXPIRED
}
