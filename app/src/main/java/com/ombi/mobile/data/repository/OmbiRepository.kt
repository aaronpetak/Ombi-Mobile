package com.ombi.mobile.data.repository

import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.api.models.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OmbiRepository @Inject constructor(
    private val api: OmbiApiService
) {
    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun multiSearch(query: String): Result<List<MultiSearchResult>> = runCatching {
        api.multiSearch(query, MultiSearchFilter()).body() ?: emptyList()
    }

    suspend fun getMovieByMovieDbId(tmdbId: Int): Result<SearchMovieViewModel> = runCatching {
        api.getMovieByMovieDbId(tmdbId).requireBody()
    }

    suspend fun getTvByMovieDbId(tmdbId: Int): Result<SearchTvShowViewModel> = runCatching {
        api.getTvByMovieDbId(tmdbId).requireBody()
    }

    suspend fun getTvByTvDbId(tvDbId: Int): Result<SearchTvShowViewModel> = runCatching {
        api.getTvByTvDbId(tvDbId).requireBody()
    }

    // ── Discover ──────────────────────────────────────────────────────────────

    suspend fun getPopularMovies(position: Int = 0, count: Int = 20): Result<List<SearchMovieViewModel>> = runCatching {
        api.getPopularMovies(position, count).body() ?: emptyList()
    }

    suspend fun getUpcomingMovies(position: Int = 0, count: Int = 20): Result<List<SearchMovieViewModel>> = runCatching {
        api.getUpcomingMovies(position, count).body() ?: emptyList()
    }

    suspend fun getTrendingTv(position: Int = 0, count: Int = 20): Result<List<SearchTvShowViewModel>> = runCatching {
        api.getTrendingTv(position, count).body() ?: emptyList()
    }

    // ── Recently Added ────────────────────────────────────────────────────────

    suspend fun getRecentlyAddedMovies(): Result<List<RecentlyAddedMovie>> = runCatching {
        api.getRecentlyAddedMovies().body() ?: emptyList()
    }

    suspend fun getRecentlyAddedTv(): Result<List<RecentlyAddedTv>> = runCatching {
        api.getRecentlyAddedTv().body() ?: emptyList()
    }

    // ── Movie Requests ────────────────────────────────────────────────────────

    suspend fun requestMovie(movieDbId: Int): Result<RequestEngineResult> = runCatching {
        api.requestMovie(MovieRequestBody(movieDbId)).requireBody()
    }

    suspend fun getMovieRequests(count: Int = 30, position: Int = 0): Result<RequestsViewModel<MovieRequest>> = runCatching {
        api.getMovieRequests(count, position).requireBody()
    }

    suspend fun cancelMovieRequest(requestId: Int): Result<Unit> = runCatching {
        api.cancelMovieRequest(requestId)
    }.map { }

    // ── TV Requests ───────────────────────────────────────────────────────────

    suspend fun requestTv(theMovieDbId: Int, requestAll: Boolean = true): Result<RequestEngineResult> = runCatching {
        api.requestTv(TvRequestBody(theMovieDbId = theMovieDbId, requestAll = requestAll)).requireBody()
    }

    suspend fun getTvRequests(count: Int = 30, position: Int = 0): Result<RequestsViewModel<TvRequest>> = runCatching {
        api.getTvRequests(count, position).requireBody()
    }

    suspend fun cancelTvRequest(requestId: Int): Result<Unit> = runCatching {
        api.cancelTvRequest(requestId)
    }.map { }

    // ── User ──────────────────────────────────────────────────────────────────

    suspend fun getCurrentUser(): Result<UserViewModel> = runCatching {
        api.getCurrentUser().requireBody()
    }
}

/**
 * Returns the response body, or throws an [Exception] with the HTTP status code and error body
 * text so callers get a meaningful message instead of a NullPointerException.
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
