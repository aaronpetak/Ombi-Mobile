package com.ombi.mobile.data.repository

import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.api.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OmbiRepository @Inject constructor(
    private val api: OmbiApiService
) {
    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun multiSearch(query: String): Result<List<MultiSearchResult>> = runCatching {
        api.multiSearch(query).body() ?: emptyList()
    }

    // ── Discover ──────────────────────────────────────────────────────────────

    suspend fun getPopularMovies(position: Int = 0, count: Int = 20): Result<List<SearchMovieViewModel>> = runCatching {
        api.getPopularMovies(position, count).body() ?: emptyList()
    }

    suspend fun getUpcomingMovies(position: Int = 0, count: Int = 20): Result<List<SearchMovieViewModel>> = runCatching {
        api.getUpcomingMovies(position, count).body() ?: emptyList()
    }

    suspend fun getNowPlayingMovies(position: Int = 0, count: Int = 20): Result<List<SearchMovieViewModel>> = runCatching {
        api.getNowPlayingMovies(position, count).body() ?: emptyList()
    }

    suspend fun getPopularTv(position: Int = 0, count: Int = 20): Result<List<SearchTvShowViewModel>> = runCatching {
        api.getPopularTv(position, count).body() ?: emptyList()
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
        api.requestMovie(MovieRequestBody(movieDbId)).body()!!
    }

    suspend fun getMovieRequests(count: Int = 30, position: Int = 0): Result<RequestsViewModel<MovieRequest>> = runCatching {
        api.getMovieRequests(count, position).body()!!
    }

    suspend fun cancelMovieRequest(requestId: Int): Result<Unit> = runCatching {
        api.cancelMovieRequest(requestId)
        Unit
    }

    // ── TV Requests ───────────────────────────────────────────────────────────

    suspend fun requestTv(tvDbId: Int, requestAll: Boolean = true): Result<RequestEngineResult> = runCatching {
        api.requestTv(TvRequestBody(tvDbId = tvDbId, requestAll = requestAll)).body()!!
    }

    suspend fun getTvRequests(count: Int = 30, position: Int = 0): Result<RequestsViewModel<TvRequest>> = runCatching {
        api.getTvRequests(count, position).body()!!
    }

    suspend fun cancelTvRequest(requestId: Int): Result<Unit> = runCatching {
        api.cancelTvRequest(requestId)
        Unit
    }

    // ── User ──────────────────────────────────────────────────────────────────

    suspend fun getCurrentUser(): Result<UserViewModel> = runCatching {
        api.getCurrentUser().body()!!
    }
}
