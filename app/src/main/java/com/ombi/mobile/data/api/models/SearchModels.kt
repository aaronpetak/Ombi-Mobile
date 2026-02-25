package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data models for the Ombi V2 search and discover API endpoints.
 *
 * [MultiSearchResult] — lightweight result from the multi-search endpoint.
 * [SearchMovieViewModel] / [SearchTvShowViewModel] — enriched detail models returned
 * by the per-item lookup endpoints; include availability and request-status flags.
 * [SeasonViewModel] / [EpisodeViewModel] — nested season/episode detail for TV shows.
 */

/**
 * Result from POST /api/v2/search/multi/{term}.
 * Note: `id` is always the TheMovieDb ID (even for TV shows), returned as a string.
 * `mediaType` is one of "movie", "tv", "person".
 */
@JsonClass(generateAdapter = true)
data class MultiSearchResult(
    @Json(name = "id")        val id: String,
    @Json(name = "mediaType") val mediaType: String,
    @Json(name = "title")     val title: String?,
    @Json(name = "poster")    val poster: String?,
    @Json(name = "overview")  val overview: String?
) {
    val isMovie: Boolean get() = mediaType == "movie"
    val isTv: Boolean   get() = mediaType == "tv"
}

/** Request body for POST /api/v2/search/multi/{term} */
@JsonClass(generateAdapter = true)
data class MultiSearchFilter(
    @Json(name = "movies")  val movies: Boolean  = true,
    @Json(name = "tvShows") val tvShows: Boolean = true,
    @Json(name = "music")   val music: Boolean   = false,
    @Json(name = "people")  val people: Boolean  = false
)

/**
 * Movie detail returned by GET /api/v2/search/movie/{theMovieDbId},
 * GET /api/v2/search/movie/popular, and similar discover endpoints.
 * Includes availability and request-status flags populated from the Ombi database.
 */
@JsonClass(generateAdapter = true)
data class SearchMovieViewModel(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String?,
    @Json(name = "releaseDate") val releaseDate: String?,
    @Json(name = "posterPath") val posterPath: String?,
    @Json(name = "backdropPath") val backdropPath: String?,
    @Json(name = "voteAverage") val voteAverage: Double?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "imdbId") val imdbId: String?,
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "requested") val requested: Boolean = false,
    @Json(name = "approved") val approved: Boolean = false,
    @Json(name = "plexUrl") val plexUrl: String?,
    @Json(name = "quality") val quality: String?
)

/**
 * TV show detail returned by GET /api/v2/search/Tv/moviedb/{id} and similar endpoints.
 * [theMovieDbId] is used by the V2 TV request endpoint; [id] is the TVDb ID.
 * [seasonRequests] is populated for per-item lookups, null for list responses.
 */
@JsonClass(generateAdapter = true)
data class SearchTvShowViewModel(
    @Json(name = "id") val id: Int,
    @Json(name = "theMovieDbId") val theMovieDbId: Int?,
    @Json(name = "title") val title: String?,
    @Json(name = "firstAired") val firstAired: String?,
    @Json(name = "banner") val banner: String?,
    @Json(name = "backdropPath") val backdropPath: String?,
    @Json(name = "rating") val rating: Double?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "requested") val requested: Boolean = false,
    @Json(name = "approved") val approved: Boolean = false,
    @Json(name = "plexUrl") val plexUrl: String?,
    @Json(name = "quality") val quality: String?,
    @Json(name = "seasonRequests") val seasonRequests: List<SeasonViewModel>?
)

@JsonClass(generateAdapter = true)
data class SeasonViewModel(
    @Json(name = "seasonNumber") val seasonNumber: Int,
    @Json(name = "episodes") val episodes: List<EpisodeViewModel>
)

@JsonClass(generateAdapter = true)
data class EpisodeViewModel(
    @Json(name = "episodeNumber") val episodeNumber: Int,
    @Json(name = "title") val title: String?,
    @Json(name = "airDate") val airDate: String?,
    @Json(name = "requested") val requested: Boolean = false
)
