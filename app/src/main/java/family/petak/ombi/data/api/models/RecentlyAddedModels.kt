package family.petak.ombi.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecentlyAddedMovie(
    @Json(name = "id") val id: Int,
    @Json(name = "theMovieDbId") val theMovieDbId: Int?,
    @Json(name = "imdbId") val imdbId: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "posterPath") val posterPath: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "addedAt") val addedAt: String?
)

@JsonClass(generateAdapter = true)
data class RecentlyAddedTv(
    @Json(name = "id") val id: Int,
    @Json(name = "tvDbId") val tvDbId: Int?,
    @Json(name = "title") val title: String?,
    @Json(name = "posterPath") val posterPath: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "addedAt") val addedAt: String?
)
