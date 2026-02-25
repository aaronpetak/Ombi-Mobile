package com.ombi.mobile.data.repository

import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.api.models.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central data access layer for all Ombi API calls (excluding auth).
 *
 * Every public function returns [Result]<T> so callers never have to
 * catch exceptions — failures are surfaced as [Result.failure] with a
 * meaningful message derived from the HTTP status code and error body.
 *
 * Network calls are suspend functions and must be invoked from a coroutine.
 */
@Singleton
class OmbiRepository @Inject constructor(
    private val api: OmbiApiService
) {

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Searches movies and TV shows simultaneously using the Ombi V2 multi-search
     * endpoint. Returns only movie and TV results (music and people are excluded
     * via the [MultiSearchFilter] request body).
     */
    suspend fun multiSearch(query: String): Result<List<MultiSearchResult>> = runCatching {
        api.multiSearch(query, MultiSearchFilter()).body() ?: emptyList()
    }

    /**
     * Fetches full movie details including availability and request status by
     * TMDB ID. Used to enrich search results before displaying the detail sheet.
     */
    suspend fun getMovieByMovieDbId(tmdbId: Int): Result<SearchMovieViewModel> = runCatching {
        api.getMovieByMovieDbId(tmdbId).requireBody()
    }

    /**
     * Fetches full TV show details including availability and request status by
     * TMDB ID. Used to enrich search results before displaying the detail sheet.
     */
    suspend fun getTvByMovieDbId(tmdbId: Int): Result<SearchTvShowViewModel> = runCatching {
        api.getTvByMovieDbId(tmdbId).requireBody()
    }

    /**
     * Resolves a TV show by its TVDb ID and returns the full details including
     * the TMDB ID. Used as a fallback for discover/recently-added items that
     * only carry a TVDb ID — the TMDB ID is required for V2 TV requests.
     */
    suspend fun getTvByTvDbId(tvDbId: Int): Result<SearchTvShowViewModel> = runCatching {
        api.getTvByTvDbId(tvDbId).requireBody()
    }

    // ── Discover ──────────────────────────────────────────────────────────────

    /** Fetches the current page of popular movies from the Ombi discover endpoint. */
    suspend fun getPopularMovies(position: Int = 0, count: Int = 20): Result<List<SearchMovieViewModel>> = runCatching {
        api.getPopularMovies(position, count).body() ?: emptyList()
    }

    /** Fetches the current page of upcoming movies from the Ombi discover endpoint. */
    suspend fun getUpcomingMovies(position: Int = 0, count: Int = 20): Result<List<SearchMovieViewModel>> = runCatching {
        api.getUpcomingMovies(position, count).body() ?: emptyList()
    }

    /** Fetches the current page of trending TV shows from the Ombi discover endpoint. */
    suspend fun getTrendingTv(position: Int = 0, count: Int = 20): Result<List<SearchTvShowViewModel>> = runCatching {
        api.getTrendingTv(position, count).body() ?: emptyList()
    }

    // ── Recently Added ────────────────────────────────────────────────────────

    /** Returns movies recently added to the connected media server (Plex/Emby/Jellyfin). */
    suspend fun getRecentlyAddedMovies(): Result<List<RecentlyAddedMovie>> = runCatching {
        api.getRecentlyAddedMovies().body() ?: emptyList()
    }

    /** Returns TV shows recently added to the connected media server (Plex/Emby/Jellyfin). */
    suspend fun getRecentlyAddedTv(): Result<List<RecentlyAddedTv>> = runCatching {
        api.getRecentlyAddedTv().body() ?: emptyList()
    }

    // ── Movie Requests ────────────────────────────────────────────────────────

    /**
     * Submits a new movie request via the Ombi V1 request engine.
     *
     * @param movieDbId The TMDB ID of the movie to request.
     * @return [RequestEngineResult] which includes a [RequestEngineResult.result]
     *         flag and an optional [RequestEngineResult.errorMessage].
     */
    suspend fun requestMovie(movieDbId: Int): Result<RequestEngineResult> = runCatching {
        api.requestMovie(MovieRequestBody(movieDbId)).requireBody()
    }

    /**
     * Returns the paginated list of movie requests visible to the current user.
     * Results are sorted by request date descending.
     */
    suspend fun getMovieRequests(count: Int = 30, position: Int = 0): Result<RequestsViewModel<MovieRequest>> = runCatching {
        api.getMovieRequests(count, position).requireBody()
    }

    /**
     * Deletes a movie request by its request ID.
     * Only admin users are permitted to delete requests on the Ombi backend.
     */
    suspend fun cancelMovieRequest(requestId: Int): Result<Unit> = runCatching {
        val response = api.cancelMovieRequest(requestId)
        if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
    }

    // ── TV Requests ───────────────────────────────────────────────────────────

    /**
     * Submits a new TV show request via the Ombi V2 request engine.
     *
     * Defaults to requesting all seasons ([requestAll] = true). The Ombi V2
     * endpoint requires [theMovieDbId] — NOT the TVDb ID. If only a TVDb ID is
     * available, resolve it first with [getTvByTvDbId].
     *
     * @param theMovieDbId The TMDB ID of the TV show to request.
     * @param requestAll   When true, all seasons and episodes are requested.
     */
    suspend fun requestTv(theMovieDbId: Int, requestAll: Boolean = true): Result<RequestEngineResult> = runCatching {
        api.requestTv(TvRequestBody(theMovieDbId = theMovieDbId, requestAll = requestAll)).requireBody()
    }

    /**
     * Returns the paginated list of TV requests visible to the current user.
     * The API returns child requests (season-level); poster images are on the
     * nested [TvRequest.parentRequest] object.
     */
    suspend fun getTvRequests(count: Int = 30, position: Int = 0): Result<RequestsViewModel<TvRequest>> = runCatching {
        api.getTvRequests(count, position).requireBody()
    }

    /**
     * Deletes a TV parent request by its parent request ID.
     * This removes all child season requests associated with the parent.
     * Only admin users are permitted to delete requests on the Ombi backend.
     *
     * @param requestId The **parent** request ID (from [TvParentRequest.id]).
     */
    suspend fun cancelTvRequest(requestId: Int): Result<Unit> = runCatching {
        val response = api.cancelTvRequest(requestId)
        if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
    }

    // ── User ──────────────────────────────────────────────────────────────────

    /**
     * Returns the full profile for the currently authenticated user, including
     * their permission claims. Used to determine admin status in the UI.
     */
    suspend fun getCurrentUser(): Result<UserViewModel> = runCatching {
        api.getCurrentUser().requireBody()
    }
}

/**
 * Helper extension for Retrofit [Response] objects.
 *
 * Returns the response body on success, or throws an [Exception] with the
 * HTTP status code and up to 300 characters of the error body text.
 * This prevents silent NullPointerExceptions from `.body()!!` calls and
 * ensures error messages are propagated meaningfully through [Result].
 */
private fun <T> Response<T>.requireBody(): T {
    if (isSuccessful) {
        return body() ?: throw Exception("Empty response body (HTTP ${code()})")
    }
    val errText = errorBody()?.string()?.take(300)?.trim()
    throw Exception(
        if (!errText.isNullOrBlank()) "HTTP ${code()}: $errText"
        else "HTTP ${code()}"
    )
}
