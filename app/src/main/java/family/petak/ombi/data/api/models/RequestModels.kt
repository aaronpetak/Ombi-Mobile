package family.petak.ombi.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Request bodies ---

@JsonClass(generateAdapter = true)
data class MovieRequestBody(
    @Json(name = "theMovieDbId") val theMovieDbId: Int
)

@JsonClass(generateAdapter = true)
data class TvRequestBody(
    @Json(name = "tvDbId") val tvDbId: Int,
    @Json(name = "requestAll") val requestAll: Boolean = false,
    @Json(name = "firstSeason") val firstSeason: Boolean = false,
    @Json(name = "latestSeason") val latestSeason: Boolean = false,
    @Json(name = "seasons") val seasons: List<SeasonRequestBody> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SeasonRequestBody(
    @Json(name = "seasonNumber") val seasonNumber: Int,
    @Json(name = "episodes") val episodes: List<EpisodeRequestBody>
)

@JsonClass(generateAdapter = true)
data class EpisodeRequestBody(
    @Json(name = "episodeNumber") val episodeNumber: Int
)

// --- API responses ---

@JsonClass(generateAdapter = true)
data class RequestEngineResult(
    @Json(name = "result") val result: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "isError") val isError: Boolean,
    @Json(name = "errorMessage") val errorMessage: String?,
    @Json(name = "requestId") val requestId: Int?
)

@JsonClass(generateAdapter = true)
data class RequestsViewModel<T>(
    @Json(name = "collection") val collection: List<T>,
    @Json(name = "total") val total: Int
)

@JsonClass(generateAdapter = true)
data class MovieRequest(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String?,
    @Json(name = "posterPath") val posterPath: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "releaseDate") val releaseDate: String?,
    @Json(name = "approved") val approved: Boolean,
    @Json(name = "denied") val denied: Boolean?,
    @Json(name = "available") val available: Boolean,
    @Json(name = "deniedReason") val deniedReason: String?,
    @Json(name = "requestedDate") val requestedDate: String?,
    @Json(name = "requestedUser") val requestedUser: RequestedUser?
) {
    val statusLabel: String get() = when {
        available -> "Available"
        denied == true -> "Denied"
        approved -> "Processing"
        else -> "Pending"
    }
}

@JsonClass(generateAdapter = true)
data class TvRequest(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String?,
    @Json(name = "posterPath") val posterPath: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "approved") val approved: Boolean,
    @Json(name = "denied") val denied: Boolean?,
    @Json(name = "available") val available: Boolean,
    @Json(name = "deniedReason") val deniedReason: String?,
    @Json(name = "requestedDate") val requestedDate: String?,
    @Json(name = "requestedUser") val requestedUser: RequestedUser?,
    @Json(name = "childRequests") val childRequests: List<ChildRequest>?
)

@JsonClass(generateAdapter = true)
data class ChildRequest(
    @Json(name = "id") val id: Int,
    @Json(name = "approved") val approved: Boolean,
    @Json(name = "denied") val denied: Boolean?,
    @Json(name = "available") val available: Boolean,
    @Json(name = "seasonRequests") val seasonRequests: List<SeasonViewModel>?
)

@JsonClass(generateAdapter = true)
data class RequestedUser(
    @Json(name = "userId") val userId: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "alias") val alias: String?
)
