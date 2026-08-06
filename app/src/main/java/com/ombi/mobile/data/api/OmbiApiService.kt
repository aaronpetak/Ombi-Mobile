package com.ombi.mobile.data.api

import com.ombi.mobile.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface for the Ombi REST API.
 *
 * The base URL is a placeholder (`http://localhost/`); the actual host is substituted
 * at runtime by [com.ombi.mobile.di.NetworkModule]'s `dynamicUrlInterceptor`, which
 * reads the user-configured server URL from [com.ombi.mobile.data.preferences.UserPreferences].
 *
 * Bearer-token authentication is added automatically by the `authInterceptor` in
 * [com.ombi.mobile.di.NetworkModule] — no manual header needed per call.
 *
 * API version note:
 * - V1 (`/api/v1/`) — auth, identity, recently-added, and request management (CRUD)
 * - V2 (`/api/v2/`) — search, discover, and paged request listings
 */
interface OmbiApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/v1/token")
    suspend fun login(@Body request: UserAuthRequest): Response<AuthTokenResponse>

    /** Alternative Plex SSO login (defined for completeness; not used by the current UI). */
    @POST("api/v1/token/plextoken")
    suspend fun loginWithPlexToken(@Body request: PlexTokenAuthRequest): Response<AuthTokenResponse>

    // ── Identity ──────────────────────────────────────────────────────────────

    @GET("api/v1/identity")
    suspend fun getCurrentUser(): Response<UserViewModel>

    // ── Search (V2) ───────────────────────────────────────────────────────────

    @POST("api/v2/search/multi/{searchTerm}")
    suspend fun multiSearch(
        @Path("searchTerm") searchTerm: String,
        @Body filter: MultiSearchFilter
    ): Response<List<MultiSearchResult>>

    /** Look up a single movie by its TheMovieDb ID (includes available/requested/approved status). */
    @GET("api/v2/search/movie/{theMovieDbId}")
    suspend fun getMovieByMovieDbId(@Path("theMovieDbId") theMovieDbId: Int): Response<SearchMovieViewModel>

    /** Look up a TV show by its TheMovieDb ID (includes available/requested/approved status). */
    @GET("api/v2/search/tv/moviedb/{theMovieDbId}")
    suspend fun getTvByMovieDbId(@Path("theMovieDbId") theMovieDbId: Int): Response<SearchTvShowViewModel>

    /** Look up a TV show by its TVDb ID (returns theMovieDbId needed for V2 requests). */
    @GET("api/v2/search/tv/{tvDbId}")
    suspend fun getTvByTvDbId(@Path("tvDbId") tvDbId: Int): Response<SearchTvShowViewModel>

    // ── Discover — Movies ─────────────────────────────────────────────────────

    @GET("api/v2/search/movie/popular/{position}/{count}")
    suspend fun getPopularMovies(
        @Path("position") position: Int,
        @Path("count") count: Int
    ): Response<List<SearchMovieViewModel>>

    @GET("api/v2/search/movie/upcoming/{position}/{count}")
    suspend fun getUpcomingMovies(
        @Path("position") position: Int,
        @Path("count") count: Int
    ): Response<List<SearchMovieViewModel>>

    // ── Discover — TV ─────────────────────────────────────────────────────────

    @GET("api/v2/search/tv/trending/{position}/{count}")
    suspend fun getTrendingTv(
        @Path("position") position: Int,
        @Path("count") count: Int
    ): Response<List<SearchTvShowViewModel>>

    // ── Recently Added ────────────────────────────────────────────────────────

    @GET("api/v1/recentlyadded/movies")
    suspend fun getRecentlyAddedMovies(): Response<List<RecentlyAddedMovie>>

    @GET("api/v1/recentlyadded/tv")
    suspend fun getRecentlyAddedTv(): Response<List<RecentlyAddedTv>>

    // ── Movie Requests ────────────────────────────────────────────────────────

    @POST("api/v1/request/movie")
    suspend fun requestMovie(@Body request: MovieRequestBody): Response<RequestEngineResult>

    @GET("api/v2/requests/movie/{count}/{position}/{sort}/{sortOrder}")
    suspend fun getMovieRequests(
        @Path("count") count: Int,
        @Path("position") position: Int,
        // Retrofit ignores default values on @Path params — callers must pass these.
        @Path("sort") sort: String,
        @Path("sortOrder") sortOrder: Int
    ): Response<RequestsViewModel<MovieRequest>>

    @DELETE("api/v1/request/movie/{requestId}")
    suspend fun cancelMovieRequest(@Path("requestId") requestId: Int): Response<Unit>

    // ── TV Requests ───────────────────────────────────────────────────────────

    @POST("api/v2/requests/tv")
    suspend fun requestTv(@Body request: TvRequestBody): Response<RequestEngineResult>

    @GET("api/v2/requests/tv/{count}/{position}/{sort}/{sortOrder}")
    suspend fun getTvRequests(
        @Path("count") count: Int,
        @Path("position") position: Int,
        // Retrofit ignores default values on @Path params — callers must pass these.
        @Path("sort") sort: String,
        @Path("sortOrder") sortOrder: Int
    ): Response<RequestsViewModel<TvRequest>>

    @DELETE("api/v1/request/tv/{requestId}")
    suspend fun cancelTvRequest(@Path("requestId") requestId: Int): Response<Unit>
}
