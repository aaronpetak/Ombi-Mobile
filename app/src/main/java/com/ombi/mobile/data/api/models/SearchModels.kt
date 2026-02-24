package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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

@JsonClass(generateAdapter = true)
data class SearchTvShowViewModel(
    @Json(name = "id") val id: Int,
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
