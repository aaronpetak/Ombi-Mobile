package family.petak.ombi.data.api

import family.petak.ombi.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface OmbiApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/v1/token")
    suspend fun login(@Body request: UserAuthRequest): Response<AuthTokenResponse>

    @POST("api/v1/token/plextoken")
    suspend fun loginWithPlexToken(@Body request: PlexTokenAuthRequest): Response<AuthTokenResponse>

    // ── Identity ──────────────────────────────────────────────────────────────

    @GET("api/v1/identity")
    suspend fun getCurrentUser(): Response<UserViewModel>

    // ── Search (V2) ───────────────────────────────────────────────────────────

    @GET("api/v2/search/multi/{searchTerm}")
    suspend fun multiSearch(@Path("searchTerm") searchTerm: String): Response<List<MultiSearchResult>>

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

    @GET("api/v2/search/movie/nowplaying/{position}/{count}")
    suspend fun getNowPlayingMovies(
        @Path("position") position: Int,
        @Path("count") count: Int
    ): Response<List<SearchMovieViewModel>>

    // ── Discover — TV ─────────────────────────────────────────────────────────

    @GET("api/v2/search/tv/popular/{position}/{count}")
    suspend fun getPopularTv(
        @Path("position") position: Int,
        @Path("count") count: Int
    ): Response<List<SearchTvShowViewModel>>

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

    @POST("api/v2/requests/movie")
    suspend fun requestMovie(@Body request: MovieRequestBody): Response<RequestEngineResult>

    @GET("api/v2/requests/movie/{count}/{position}/{sort}/{sortOrder}")
    suspend fun getMovieRequests(
        @Path("count") count: Int,
        @Path("position") position: Int,
        @Path("sort") sort: String = "requesteddate",
        @Path("sortOrder") sortOrder: Int = 1
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
        @Path("sort") sort: String = "requesteddate",
        @Path("sortOrder") sortOrder: Int = 1
    ): Response<RequestsViewModel<TvRequest>>

    @DELETE("api/v1/request/tv/{requestId}")
    suspend fun cancelTvRequest(@Path("requestId") requestId: Int): Response<Unit>
}
