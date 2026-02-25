package com.ombi.mobile.ui.model

import com.ombi.mobile.data.api.models.MultiSearchResult
import com.ombi.mobile.data.api.models.RecentlyAddedMovie
import com.ombi.mobile.data.api.models.RecentlyAddedTv
import com.ombi.mobile.data.api.models.SearchMovieViewModel
import com.ombi.mobile.data.api.models.SearchTvShowViewModel

/**
 * Unified UI model representing a movie or TV show.
 *
 * The Ombi API returns several different response types depending on the
 * endpoint (search, discover, recently-added, etc.). [MediaItem] normalises
 * all of them into a single shape that the UI layer can work with uniformly,
 * so screens and components don't need to know which API model they came from.
 *
 * ID strategy:
 * - Movies always have a [theMovieDbId]; [tvDbId] is null.
 * - TV shows may have both, one, or neither depending on the source endpoint.
 *   [theMovieDbId] is required to submit a V2 TV request; if only [tvDbId] is
 *   present, it must be resolved via [com.ombi.mobile.data.repository.OmbiRepository.getTvByTvDbId].
 */
data class MediaItem(
    val title: String,
    val posterPath: String?,
    val overview: String?,
    val year: String?,
    val rating: Double?,
    val isMovie: Boolean,
    val theMovieDbId: Int?,
    val tvDbId: Int?,
    val available: Boolean,
    val requested: Boolean,
    val approved: Boolean,
    val denied: Boolean = false
)

/**
 * Maps a [SearchMovieViewModel] (returned by search and discover endpoints)
 * to a [MediaItem]. The movie's TMDB [SearchMovieViewModel.id] becomes
 * [MediaItem.theMovieDbId].
 */
fun SearchMovieViewModel.toMediaItem() = MediaItem(
    title        = title ?: "",
    posterPath   = posterPath,
    overview     = overview,
    year         = releaseDate?.take(4),
    rating       = voteAverage,
    isMovie      = true,
    theMovieDbId = id,
    tvDbId       = null,
    available    = available,
    requested    = requested,
    approved     = approved
)

/**
 * Maps a [SearchTvShowViewModel] (returned by search and discover endpoints)
 * to a [MediaItem].
 *
 * Note: [SearchTvShowViewModel.backdropPath] is used as the poster because
 * the TV search response uses that field for the show image rather than
 * a dedicated posterPath.
 */
fun SearchTvShowViewModel.toMediaItem() = MediaItem(
    title        = title ?: "",
    posterPath   = backdropPath,
    overview     = overview,
    year         = firstAired?.take(4),
    rating       = rating,
    isMovie      = false,
    theMovieDbId = theMovieDbId,
    tvDbId       = id,
    available    = available,
    requested    = requested,
    approved     = approved
)

/**
 * Maps a [MultiSearchResult] (from the multi-search endpoint) to a [MediaItem].
 *
 * Multi-search results carry minimal data — no availability status, rating,
 * or year. These are filled in lazily when the user taps the item, triggering
 * a detail fetch in [com.ombi.mobile.ui.screens.search.SearchViewModel].
 *
 * The [MultiSearchResult.id] is the TMDB ID (as a string) for both movies
 * and TV shows. For TV shows, [MediaItem.tvDbId] is left null and resolved
 * later if needed.
 */
fun MultiSearchResult.toMediaItem() = MediaItem(
    title        = title ?: "",
    posterPath   = poster,
    overview     = overview,
    year         = null,
    rating       = null,
    isMovie      = isMovie,
    theMovieDbId = id.toIntOrNull(),
    tvDbId       = null,   // multi-search only returns the TMDB ID; tvDbId resolved on request
    available    = false,
    requested    = false,
    approved     = false
)

/**
 * Maps a [RecentlyAddedMovie] to a [MediaItem].
 * Recently-added movies are always [available] = true since they are already
 * in the media library.
 */
fun RecentlyAddedMovie.toMediaItem() = MediaItem(
    title        = title ?: "",
    posterPath   = posterPath,
    overview     = overview,
    year         = null,
    rating       = null,
    isMovie      = true,
    theMovieDbId = theMovieDbId,
    tvDbId       = null,
    available    = true,
    requested    = false,
    approved     = false
)

/**
 * Maps a [RecentlyAddedTv] to a [MediaItem].
 * Recently-added TV items carry a TVDb ID only; [theMovieDbId] is null and
 * must be resolved before submitting a request.
 */
fun RecentlyAddedTv.toMediaItem() = MediaItem(
    title        = title ?: "",
    posterPath   = posterPath,
    overview     = overview,
    year         = null,
    rating       = null,
    isMovie      = false,
    theMovieDbId = null,
    tvDbId       = tvDbId,
    available    = true,
    requested    = false,
    approved     = false
)
