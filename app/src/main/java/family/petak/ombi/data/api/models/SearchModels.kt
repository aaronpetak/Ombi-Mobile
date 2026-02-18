package family.petak.ombi.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Result from POST /api/v2/search/multi/{term} */
@JsonClass(generateAdapter = true)
data class MultiSearchResult(
    @Json(name = "title") val title: String?,
    @Json(name = "id") val id: Int,
    @Json(name = "poster") val poster: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "releaseDate") val releaseDate: String?,
    /** 0 = Movie, 1 = TvShow */
    @Json(name = "type") val type: Int,
    @Json(name = "imdbId") val imdbId: String?,
    @Json(name = "theMovieDbId") val theMovieDbId: Int?,
    @Json(name = "tvDbId") val tvDbId: Int?,
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "requested") val requested: Boolean = false,
    @Json(name = "approved") val approved: Boolean = false,
    @Json(name = "plexUrl") val plexUrl: String?,
    @Json(name = "quality") val quality: String?
) {
    val isMovie: Boolean get() = type == 0
    val isTv: Boolean get() = type == 1
}

@JsonClass(generateAdapter = true)
data class SearchMovieViewModel(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String?,
    @Json(name = "releaseDate") val releaseDate: String?,
    @Json(name = "poster") val poster: String?,
    @Json(name = "backdrop") val backdrop: String?,
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
    @Json(name = "poster") val poster: String?,
    @Json(name = "backdrop") val backdrop: String?,
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
