package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Request bodies ────────────────────────────────────────────────────────────

/**
 * Request body for POST /api/v1/request/movie.
 *
 * [languageCode] must be explicitly set — sending null overrides the Ombi
 * server default and causes cascade null errors on the C# side.
 */
@JsonClass(generateAdapter = true)
data class MovieRequestBody(
    @Json(name = "theMovieDbId") val theMovieDbId: Int,
    @Json(name = "languageCode") val languageCode: String = "en"
)

/**
 * Request body for POST /api/v2/requests/tv.
 *
 * [theMovieDbId] is required — the V2 TV request endpoint does NOT accept a
 * TVDb ID. If only a TVDb ID is available, resolve it first using
 * GET /api/v2/search/tv/{tvDbId}.
 *
 * [requestAll] = true requests every season and episode in one call, which is
 * the default behavior for this app.
 */
@JsonClass(generateAdapter = true)
data class TvRequestBody(
    @Json(name = "theMovieDbId")  val theMovieDbId: Int,
    @Json(name = "requestAll")    val requestAll: Boolean = false,
    @Json(name = "firstSeason")   val firstSeason: Boolean = false,
    @Json(name = "latestSeason")  val latestSeason: Boolean = false,
    @Json(name = "seasons")       val seasons: List<SeasonRequestBody> = emptyList(),
    @Json(name = "languageCode")  val languageCode: String = "en"
)

/** Specifies a single season's episodes to include in a [TvRequestBody]. */
@JsonClass(generateAdapter = true)
data class SeasonRequestBody(
    @Json(name = "seasonNumber") val seasonNumber: Int,
    @Json(name = "episodes")     val episodes: List<EpisodeRequestBody>
)

/** Specifies a single episode to include in a [SeasonRequestBody]. */
@JsonClass(generateAdapter = true)
data class EpisodeRequestBody(
    @Json(name = "episodeNumber") val episodeNumber: Int
)

// ── API responses ─────────────────────────────────────────────────────────────

/**
 * Response from the Ombi request engine (both movie and TV request endpoints).
 *
 * Always check [result] before showing a success message — the API can return
 * HTTP 200 with [result] = false when a request is a duplicate or blocked.
 */
@JsonClass(generateAdapter = true)
data class RequestEngineResult(
    @Json(name = "result")       val result: Boolean,
    @Json(name = "message")      val message: String?,
    @Json(name = "isError")      val isError: Boolean,
    @Json(name = "errorMessage") val errorMessage: String?,
    @Json(name = "requestId")    val requestId: Int?
)

/**
 * Paginated wrapper returned by the list-requests endpoints.
 *
 * @param T The request type: [MovieRequest] or [TvRequest].
 */
@JsonClass(generateAdapter = true)
data class RequestsViewModel<T>(
    @Json(name = "collection") val collection: List<T>,
    @Json(name = "total")      val total: Int
)

/**
 * A single movie request as returned by GET /api/v2/requests/movie.
 *
 * [statusLabel] is a convenience computed property for displaying the request
 * status in the UI without a series of null checks at the call site.
 */
@JsonClass(generateAdapter = true)
data class MovieRequest(
    @Json(name = "id")            val id: Int,
    @Json(name = "title")         val title: String?,
    @Json(name = "posterPath")    val posterPath: String?,
    @Json(name = "overview")      val overview: String?,
    @Json(name = "releaseDate")   val releaseDate: String?,
    @Json(name = "approved")      val approved: Boolean,
    @Json(name = "denied")        val denied: Boolean?,
    @Json(name = "available")     val available: Boolean,
    @Json(name = "deniedReason")  val deniedReason: String?,
    @Json(name = "requestedDate") val requestedDate: String?,
    @Json(name = "requestedUser") val requestedUser: RequestedUser?
) {
    /** Human-readable status string derived from the request's boolean flags. */
    val statusLabel: String get() = when {
        available      -> "Available"
        denied == true -> "Denied"
        approved       -> "Processing"
        else           -> "Pending"
    }
}

/**
 * A child TV request (season-level) as returned by GET /api/v2/requests/tv.
 *
 * The Ombi TV request API returns ChildRequests[], not the top-level parent
 * TvRequests. Status flags (approved, denied, available) are on the child.
 * Poster images are on the nested [parentRequest] object.
 *
 * [posterPath] is a computed property that falls back through the parent's
 * posterPath and background fields so the caller never needs to know about
 * the nesting.
 */
@JsonClass(generateAdapter = true)
data class TvRequest(
    @Json(name = "id")             val id: Int,
    @Json(name = "title")          val title: String?,
    @Json(name = "approved")       val approved: Boolean,
    @Json(name = "denied")         val denied: Boolean?,
    @Json(name = "available")      val available: Boolean,
    @Json(name = "deniedReason")   val deniedReason: String?,
    @Json(name = "requestedDate")  val requestedDate: String?,
    @Json(name = "requestedUser")  val requestedUser: RequestedUser?,
    @Json(name = "seasonRequests") val seasonRequests: List<SeasonViewModel>?,
    @Json(name = "parentRequest")  val parentRequest: TvParentRequest?
) {
    /** Poster path sourced from the parent request (where images actually live). */
    val posterPath: String? get() = parentRequest?.posterPath ?: parentRequest?.background

    /**
     * Human-readable status string derived from the request's boolean flags.
     * Priority: Available > Denied > Processing (approved, not yet available) > Pending.
     */
    val statusLabel: String get() = when {
        available      -> "Available"
        denied == true -> "Denied"
        approved       -> "Processing"
        else           -> "Pending"
    }
}

/**
 * The parent TV request object nested inside each [TvRequest].
 *
 * Contains the show-level metadata (title, poster) shared by all child
 * season requests. The [id] here is the parent request ID required for
 * deletion — DELETE /api/v1/request/tv/{id} expects this ID, not the
 * child's ID.
 */
@JsonClass(generateAdapter = true)
data class TvParentRequest(
    @Json(name = "id")         val id: Int,
    @Json(name = "title")      val title: String?,
    @Json(name = "posterPath") val posterPath: String?,
    @Json(name = "background") val background: String?,
    @Json(name = "tvDbId")     val tvDbId: Int?
)

/** The user who submitted a request, embedded in [MovieRequest] and [TvRequest]. */
@JsonClass(generateAdapter = true)
data class RequestedUser(
    @Json(name = "userId")   val userId: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "alias")    val alias: String?
)
