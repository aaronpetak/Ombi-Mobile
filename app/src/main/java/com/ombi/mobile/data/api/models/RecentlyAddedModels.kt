package com.ombi.mobile.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a movie that was recently added to Plex/Emby/Jellyfin via Ombi.
 *
 * Returned by GET /api/v1/recentlyadded/movies.
 * [posterPath] is a relative TMDB path — prepend the TMDB image base URL before
 * displaying (see [com.ombi.mobile.ui.components.toTmdbUrl]).
 */
@JsonClass(generateAdapter = true)
data class RecentlyAddedMovie(
    @Json(name = "id")           val id: Int,
    @Json(name = "theMovieDbId") val theMovieDbId: Int?,
    @Json(name = "imdbId")       val imdbId: String?,
    @Json(name = "title")        val title: String?,
    @Json(name = "posterPath")   val posterPath: String?,
    @Json(name = "overview")     val overview: String?,
    @Json(name = "addedAt")      val addedAt: String?
)

/**
 * Represents a TV show (or episode batch) that was recently added to the media server.
 *
 * Returned by GET /api/v1/recentlyadded/tv.
 * Note: recently-added TV items carry a [tvDbId] (TVDb) rather than a TMDB ID.
 * A TMDB ID lookup is required if the user taps the item to request it.
 */
@JsonClass(generateAdapter = true)
data class RecentlyAddedTv(
    @Json(name = "id")         val id: Int,
    @Json(name = "tvDbId")     val tvDbId: Int?,
    @Json(name = "title")      val title: String?,
    @Json(name = "posterPath") val posterPath: String?,
    @Json(name = "overview")   val overview: String?,
    @Json(name = "addedAt")    val addedAt: String?
)
