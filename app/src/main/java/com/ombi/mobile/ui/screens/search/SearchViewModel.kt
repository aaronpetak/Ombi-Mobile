package com.ombi.mobile.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.api.models.MultiSearchResult
import com.ombi.mobile.data.repository.OmbiRepository
import com.ombi.mobile.ui.model.MediaItem
import com.ombi.mobile.ui.model.toMediaItem
import com.ombi.mobile.ui.model.toRequestOutcome
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Filter applied to search results to show all, only movies, or only TV shows. */
enum class SearchFilter { ALL, MOVIES, TV }

/**
 * UI state for the Search screen.
 *
 * The screen derives the visible list from [results] and [filter] via
 * [filterResults], memoized in the composable with `remember(results, filter)`
 * so filtering runs only when one of those actually changes — not on every
 * recomposition, as a computed property here would.
 *
 * [isStatusLoading] is true while the detail fetch (availability + request
 * status) is in progress after the user taps a result card. The sheet opens
 * immediately with basic data and updates once the enriched data arrives.
 */
data class SearchUiState(
    val query:          String                   = "",
    val results:        List<MultiSearchResult>  = emptyList(),
    val filter:         SearchFilter             = SearchFilter.ALL,
    val isLoading:      Boolean                  = false,
    val error:          String?                  = null,
    val selectedItem:   MediaItem?               = null,
    val isStatusLoading: Boolean                 = false,
    val isRequesting:   Boolean                  = false,
    val requestMessage: String?                  = null
)

/** Applies [filter] to [results]. Pure; call inside `remember(results, filter)`. */
fun filterResults(results: List<MultiSearchResult>, filter: SearchFilter): List<MultiSearchResult> =
    when (filter) {
        SearchFilter.ALL    -> results
        SearchFilter.MOVIES -> results.filter { it.isMovie }
        SearchFilter.TV     -> results.filter { it.isTv }
    }

/**
 * ViewModel for the Search screen.
 *
 * Search is debounced by 400 ms and only triggers when the query is at least
 * 2 characters long. [kotlinx.coroutines.flow.collectLatest] cancels any
 * in-flight search when a new query arrives, preventing stale results.
 *
 * When the user taps a result card:
 * 1. The sheet opens immediately with the basic [MediaItem] from the search result.
 * 2. [fetchItemStatus] fetches full details (availability, request status) in the
 *    background and updates [SearchUiState.selectedItem] once complete.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: OmbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Internal flow that drives the debounced search pipeline
    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(400)               // wait 400 ms after the last keystroke
                .filter { it.length >= 2 }   // minimum 2 characters before searching
                .distinctUntilChanged()      // skip if the query hasn't actually changed
                .collectLatest { query -> performSearch(query) }
        }
    }

    /**
     * Called on every keystroke in the search field.
     * Updates the displayed query and pushes the new value into the debounce pipeline.
     * Clears results immediately when the field is emptied.
     */
    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null)
        queryFlow.value = query
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList())
        }
    }

    /** Updates the active content-type filter (All / Movies / TV). */
    fun onFilterChange(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    /**
     * Opens the detail sheet for [item] and begins a background fetch to
     * enrich the item with full availability and request-status data.
     * Passing null dismisses the sheet.
     */
    fun selectItem(item: MediaItem?) {
        _uiState.value = _uiState.value.copy(
            selectedItem    = item,
            requestMessage  = null,
            isStatusLoading = item != null   // show loading state only when opening
        )
        if (item != null) {
            viewModelScope.launch { fetchItemStatus(item) }
        }
    }

    /**
     * Fetches full item details (availability, request status) from the Ombi
     * detail endpoint and updates [SearchUiState.selectedItem].
     *
     * If the user dismisses the sheet or opens a different item before the fetch
     * completes, the update is dropped (guarded by an identity check on
     * [SearchUiState.selectedItem]).
     */
    private suspend fun fetchItemStatus(item: MediaItem) {
        val enriched = if (item.isMovie) {
            item.theMovieDbId?.let { tmdbId ->
                repository.getMovieByMovieDbId(tmdbId).getOrNull()?.toMediaItem()
            }
        } else {
            item.theMovieDbId?.let { tmdbId ->
                // Preserve the original TMDB ID in case the TV response doesn't include it
                repository.getTvByMovieDbId(tmdbId).getOrNull()?.toMediaItem()
                    ?.let { if (it.theMovieDbId == null) it.copy(theMovieDbId = tmdbId) else it }
            }
        }
        // Only apply the enrichment if the still-selected item is the same one we
        // fetched for. A slow fetch for item A must not overwrite item B after the
        // user has switched selection.
        val current = _uiState.value.selectedItem
        if (current != null &&
            current.theMovieDbId == item.theMovieDbId &&
            current.isMovie == item.isMovie
        ) {
            _uiState.value = _uiState.value.copy(
                selectedItem    = enriched ?: item,
                isStatusLoading = false
            )
        }
    }

    /**
     * Submits a media request for the currently selected item.
     * For TV shows, [MediaItem.theMovieDbId] must be non-null (it is guaranteed
     * by the time the Request button is enabled, since [fetchItemStatus] runs first).
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
                item.theMovieDbId?.let { repository.requestTv(it) }
            }
            // Shared message + optimistic-requested mapping (see toRequestOutcome).
            // Only rewrite selectedItem on a successful call, matching the
            // original: a failed/unresolved request must not overwrite an item
            // that fetchItemStatus may have enriched in the meantime.
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

    /** Executes the actual API search call and updates results. */
    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.multiSearch(query)
            .onSuccess { _uiState.value = _uiState.value.copy(results = it, isLoading = false) }
            .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoading = false) }
    }
}
