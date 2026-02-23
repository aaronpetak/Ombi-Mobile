package com.ombi.mobile.ui.model

import com.ombi.mobile.data.api.models.MultiSearchResult
import com.ombi.mobile.data.api.models.RecentlyAddedMovie
import com.ombi.mobile.data.api.models.RecentlyAddedTv
import com.ombi.mobile.data.api.models.SearchMovieViewModel
import com.ombi.mobile.data.api.models.SearchTvShowViewModel

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

fun SearchMovieViewModel.toMediaItem() = MediaItem(
    title = title ?: "",
    posterPath = poster,
    overview = overview,
    year = releaseDate?.take(4),
    rating = voteAverage,
    isMovie = true,
    theMovieDbId = id,
    tvDbId = null,
    available = available,
    requested = requested,
    approved = approved
)

fun SearchTvShowViewModel.toMediaItem() = MediaItem(
    title = title ?: "",
    posterPath = poster,
    overview = overview,
    year = firstAired?.take(4),
    rating = rating,
    isMovie = false,
    theMovieDbId = null,
    tvDbId = id,
    available = available,
    requested = requested,
    approved = approved
)

fun MultiSearchResult.toMediaItem() = MediaItem(
    title = title ?: "",
    posterPath = poster,
    overview = overview,
    year = releaseDate?.take(4),
    rating = null,
    isMovie = isMovie,
    theMovieDbId = theMovieDbId,
    tvDbId = tvDbId,
    available = available,
    requested = requested,
    approved = approved
)

fun RecentlyAddedMovie.toMediaItem() = MediaItem(
    title = title ?: "",
    posterPath = posterPath,
    overview = overview,
    year = null,
    rating = null,
    isMovie = true,
    theMovieDbId = theMovieDbId,
    tvDbId = null,
    available = true,
    requested = false,
    approved = false
)

fun RecentlyAddedTv.toMediaItem() = MediaItem(
    title = title ?: "",
    posterPath = posterPath,
    overview = overview,
    year = null,
    rating = null,
    isMovie = false,
    theMovieDbId = null,
    tvDbId = tvDbId,
    available = true,
    requested = false,
    approved = false
)
