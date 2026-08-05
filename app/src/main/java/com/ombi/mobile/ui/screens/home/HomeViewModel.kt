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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

            _uiState.value = _uiState.value.copy(
                recentMovies   = recentMoviesResult.getOrDefault(emptyList()),
                recentTv       = recentTvResult.getOrDefault(emptyList()),
                popularMovies  = popularMoviesResult.getOrDefault(emptyList()),
                trendingTv     = trendingTvResult.getOrDefault(emptyList()),
                upcomingMovies = upcomingMoviesResult.getOrDefault(emptyList()),
                error          = if (allFailed) {
                    allResults.firstNotNullOfOrNull { it.exceptionOrNull()?.message }
                        ?: "Failed to load content. Check your connection and server URL."
                } else null,
                isLoading      = false
            )
        }
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
     * For movies, uses the TMDB ID directly. For TV shows, [theMovieDbId] is
     * required by the Ombi V2 endpoint — if only a TVDb ID is available
     * (common for recently-added TV), the TMDB ID is resolved via a secondary
     * lookup before the request is submitted.
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
                // theMovieDbId is required for V2 TV requests; discover endpoints may only
                // populate tvDbId, so resolve the TMDB ID via a TVDb lookup if needed
                val tmdbId = item.theMovieDbId
                    ?: item.tvDbId?.let { repository.getTvByTvDbId(it).getOrNull()?.theMovieDbId }
                tmdbId?.let { repository.requestTv(it) }
            }
            result?.fold(
                onSuccess = { engineResult ->
                    val msg = if (engineResult.result) "Request submitted!" else "Failed: ${engineResult.errorMessage}"
                    _uiState.value = _uiState.value.copy(
                        isRequesting  = false,
                        requestMessage = msg,
                        // Optimistically mark the item as requested so the button updates immediately
                        selectedItem  = if (engineResult.result) item.copy(requested = true) else item
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isRequesting  = false,
                        requestMessage = "Error: ${it.message}"
                    )
                }
            ) ?: run {
                // result is null when neither theMovieDbId nor tvDbId could be resolved
                _uiState.value = _uiState.value.copy(isRequesting = false, requestMessage = "Missing ID for request")
            }
        }
    }
}
