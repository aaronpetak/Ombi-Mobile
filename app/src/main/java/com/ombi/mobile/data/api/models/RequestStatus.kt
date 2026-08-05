package com.ombi.mobile.data.api.models

/**
 * The display status of a media request, derived from its boolean flags.
 *
 * This is the single source of truth for the status-derivation logic shared by
 * [MovieRequest] and [TvRequest]. It is intentionally free of any UI/Compose
 * types — the colour used to render each status is resolved in the UI layer
 * (see the requests screen) so this stays in the data-model module.
 *
 * @property label Human-readable text shown to the user.
 */
enum class RequestStatus(val label: String) {
    AVAILABLE("Available"),
    DENIED("Denied"),
    PROCESSING("Processing"),
    PENDING("Pending");

    companion object {
        /**
         * Derives the status from a request's flags.
         * Priority: available > denied > approved (processing) > pending.
         */
        fun from(available: Boolean, denied: Boolean?, approved: Boolean): RequestStatus = when {
            available      -> AVAILABLE
            denied == true -> DENIED
            approved       -> PROCESSING
            else           -> PENDING
        }
    }
}
