package com.ombi.mobile.ui.model

import com.ombi.mobile.data.api.models.RequestEngineResult

/**
 * The result of submitting a media request, in a form ready to fold into a
 * screen's UI state. Produced by [toRequestOutcome].
 *
 * This captures the request-handling logic that was previously duplicated
 * verbatim in both HomeViewModel and SearchViewModel (the fold over the
 * repository Result plus the user-facing message and optimistic-requested
 * decision). The screens differ only in how they resolve the media ID and
 * which UiState they copy into, so those parts stay in each ViewModel.
 *
 * @property message        The user-facing status message to display.
 * @property isSuccess       True when the request call itself completed (the
 *                           repository returned success), regardless of whether
 *                           the engine accepted it. False for a thrown failure
 *                           or an unresolved ID.
 * @property markRequested   True only when the engine accepted the request, so
 *                           the item should be optimistically shown as requested.
 */
data class RequestOutcome(
    val message: String,
    val isSuccess: Boolean,
    val markRequested: Boolean
)

/**
 * Maps a repository request [Result] to a [RequestOutcome].
 *
 * A null receiver means no request could be made because the media ID could
 * not be resolved. On an HTTP-200-but-not-`result` response the engine's
 * [RequestEngineResult.errorMessage] is surfaced; on a thrown failure the
 * exception message is. Only a true [RequestEngineResult.result] sets
 * [RequestOutcome.markRequested].
 *
 * [isSuccess] mirrors the original per-screen logic where the selected item was
 * only rewritten in the success branch: callers should leave the selected item
 * untouched when [isSuccess] is false so a failed or unresolved request does not
 * clobber a sheet the user has since dismissed or an item enriched meanwhile.
 */
fun Result<RequestEngineResult>?.toRequestOutcome(): RequestOutcome {
    if (this == null) {
        return RequestOutcome(message = "Missing ID for request", isSuccess = false, markRequested = false)
    }
    return fold(
        onSuccess = { engineResult ->
            RequestOutcome(
                message = if (engineResult.result) "Request submitted!"
                          else "Failed: ${engineResult.errorMessage}",
                isSuccess = true,
                markRequested = engineResult.result
            )
        },
        onFailure = { RequestOutcome(message = "Error: ${it.message}", isSuccess = false, markRequested = false) }
    )
}
