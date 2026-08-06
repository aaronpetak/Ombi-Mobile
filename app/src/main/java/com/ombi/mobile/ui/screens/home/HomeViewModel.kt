package com.ombi.mobile.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.api.models.RecentlyAddedMovie
import com.ombi.mobile.data.api.models.RecentlyAddedTv
import com.ombi.mobile.data.api.models.SearchMovieViewModel
import com.ombi.mobile.data.api.models.SearchTvShowViewModel
import com.ombi.mobile.data.repository.OmbiRepository
import com.ombi.mobile.ui.model.MediaItem
import com.ombi.mobile.ui.model.toRequestOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Max items per row — bounds the per-item enrichment lookups on the recently-added rows. */
private const val HOME_ROW_LIMIT = 20

/**
 * UI state for the Home screen.
 *
 * Holds five content rows that are loaded in parallel on launch:
 * recently-added movies and TV, popular movies, trending TV, and upcoming movies.
 *
 * [selectedItem] drives the detail bottom sheet — null means no sheet is visible.
 * [isRequesting] and [requestMessage] reflect the in-flight request state shown
 * inside the sheet.
 */
data class HomeUiState(
    val recentMovies:   List<RecentlyAddedMovie>       = emptyList(),
    val recentTv:       List<RecentlyAddedTv>          = emptyList(),
    val popularMovies:  List<SearchMovieViewModel>     = emptyList(),
    val trendingTv:     List<SearchTvShowViewModel>    = emptyList(),
    val upcomingMovies: List<SearchMovieViewModel>     = emptyList(),
    val isLoading:      Boolean                        = false,
    val error:          String?                        = null,
    val selectedItem:   MediaItem?                     = null,
    val isRequesting:   Boolean                        = false,
    val requestMessage: String?                        = null
)

/**
 * ViewModel for the Home / Discover screen.
 *
 * On initialisation (and on pull-to-refresh) all five content rows are fetched
 * concurrently using [async] / [await]. Individual row failures are silently
 * swallowed — the row simply won't appear rather than crashing the whole screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OmbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Refreshes all content rows concurrently. Safe to call multiple times. */
    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Fan-out: all five requests run in parallel
                val recentMovies   = async { repository.getRecentlyAddedMovies() }
                val recentTv       = async { repository.getRecentlyAddedTv() }
                val popularMovies  = async { repository.getPopularMovies() }
                val trendingTv     = async { repository.getTrendingTv() }
                val upcomingMovies = async { repository.getUpcomingMovies() }

                val recentMoviesResult   = recentMovies.await()
                val recentTvResult       = recentTv.await()
                val popularMoviesResult  = popularMovies.await()
                val trendingTvResult     = trendingTv.await()
                val upcomingMoviesResult = upcomingMovies.await()

                // If every row failed, surface an error rather than showing five silent
                // empty rows. Partial failures still degrade gracefully to empty rows.
                val allResults = listOf(
                    recentMoviesResult, recentTvResult, popularMoviesResult,
                    trendingTvResult, upcomingMoviesResult
                )
                val allFailed = allResults.all { it.isFailure }

                // Dedupe on id: Ombi's recentlyadded endpoints can return the same
                // TMDB id more than once, and the LazyRows key on it.id — duplicate
                // keys crash Compose ("Key … was already used"). distinctBy keeps the
                // first occurrence, using the exact id the row keys rely on.
                //
                // The recentlyadded endpoints don't include posterPath or overview
                // (Ombi's own web client fills these in client-side), so enrich each
                // item via the TMDB detail endpoint it already carries theMovieDbId for.
                // Items whose lookup fails are kept as-is (title only, no poster).
                val recentMoviesEnriched = enrichRecentMovies(
                    recentMoviesResult.getOrDefault(emptyList()).distinctBy { it.id }.take(HOME_ROW_LIMIT)
                )
                val recentTvEnriched = enrichRecentTv(
                    recentTvResult.getOrDefault(emptyList()).distinctBy { it.id }.take(HOME_ROW_LIMIT)
                )

                _uiState.value = _uiState.value.copy(
                    recentMovies   = recentMoviesEnriched,
                    recentTv       = recentTvEnriched,
                    popularMovies  = popularMoviesResult.getOrDefault(emptyList()).distinctBy { it.id },
                    trendingTv     = trendingTvResult.getOrDefault(emptyList()).distinctBy { it.id },
                    upcomingMovies = upcomingMoviesResult.getOrDefault(emptyList()).distinctBy { it.id },
                    error          = if (allFailed) {
                        allResults.firstNotNullOfOrNull { it.exceptionOrNull()?.message }
                            ?: "Failed to load content. Check your connection and server URL."
                    } else null
                )
            } finally {
                // Guarantee the spinner clears even if the load is cancelled mid-flight
                // (e.g. an await() interrupted before the state write above), which would
                // otherwise leave a permanent loading spinner.
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Enriches recently-added movies with poster and overview via the TMDB detail
     * endpoint. The recentlyadded response carries neither field, but every item
     * has a [RecentlyAddedMovie.theMovieDbId] we can look up. Lookups run in
     * parallel; an item with no id or a failed lookup is returned unchanged.
     */
    private suspend fun enrichRecentMovies(items: List<RecentlyAddedMovie>): List<RecentlyAddedMovie> =
        coroutineScope {
            items.map { movie ->
                async {
                    val tmdbId = movie.theMovieDbId ?: return@async movie
                    val detail = repository.getMovieByMovieDbId(tmdbId).getOrNull() ?: return@async movie
                    movie.copy(
                        posterPath = movie.posterPath ?: detail.posterPath,
                        overview   = movie.overview ?: detail.overview
                    )
                }
            }.awaitAll()
        }

    /**
     * Enriches recently-added TV shows with poster and overview via the TMDB detail
     * endpoint. See [enrichRecentMovies]; the TV detail response exposes the portrait
     * poster via `posterPath` or, on the per-item endpoint, `images.original`.
     */
    private suspend fun enrichRecentTv(items: List<RecentlyAddedTv>): List<RecentlyAddedTv> =
        coroutineScope {
            items.map { tv ->
                async {
                    val tmdbId = tv.theMovieDbId ?: return@async tv
                    val detail = repository.getTvByMovieDbId(tmdbId).getOrNull() ?: return@async tv
                    tv.copy(
                        posterPath = tv.posterPath ?: detail.posterPath ?: detail.images?.original,
                        overview   = tv.overview ?: detail.overview
                    )
                }
            }.awaitAll()
        }

    /**
     * Opens or closes the detail bottom sheet for [item].
     * Passing null dismisses the sheet and clears any request message.
     */
    fun selectItem(item: MediaItem?) {
        _uiState.value = _uiState.value.copy(selectedItem = item, requestMessage = null)
    }

    /**
     * Submits a media request for the currently selected item.
     *
     * Both movie and TV requests use the TMDB ID directly — every source endpoint
     * (search, discover, recently-added) supplies it, so no secondary lookup is needed.
     */
    fun requestSelected() {
        val item = _uiState.value.selectedItem ?: return
        // Guard before launching: setting isRequesting inside the coroutine let a fast
        // second tap pass this check and submit a duplicate request.
        if (_uiState.value.isRequesting) return
        _uiState.value = _uiState.value.copy(isRequesting = true, requestMessage = null)
        viewModelScope.launch {
            val result = if (item.isMovie) {
                item.theMovieDbId?.let { repository.requestMovie(it) }
            } else {
                // Every TV endpoint (search, discover, recently-added) supplies the TMDB ID
                // that the V2 request endpoint needs, so no secondary resolution is required.
                item.theMovieDbId?.let { repository.requestTv(it) }
            }
            // Shared message + optimistic-requested mapping (see toRequestOutcome).
            // Only rewrite selectedItem on a successful call, matching the
            // original: a failed/unresolved request must not clobber a sheet the
            // user may have dismissed meanwhile.
            val outcome = result.toRequestOutcome()
            _uiState.value = _uiState.value.copy(
                isRequesting   = false,
                requestMessage = outcome.message,
                selectedItem   = if (outcome.isSuccess) {
                    if (outcome.markRequested) item.copy(requested = true) else item
                } else {
                    _uiState.value.selectedItem
                }
            )
        }
    }
}
