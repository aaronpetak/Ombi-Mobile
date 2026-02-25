package com.ombi.mobile.ui.screens.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.api.models.MovieRequest
import com.ombi.mobile.data.api.models.TvRequest
import com.ombi.mobile.data.repository.OmbiRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Primary tab: Movies vs. TV shows. */
enum class RequestTab { MOVIES, TV }

/** Secondary tab: requests still in progress vs. those that have been fulfilled or denied. */
enum class StatusTab { PENDING, PROCESSED }

/**
 * UI state for the Requests screen.
 *
 * Derived list properties ([pendingMovies], [processedMovies], etc.) split the
 * raw request lists so the UI only has to pick [visibleMovies] or [visibleTv]
 * based on the active tabs — no filtering logic needed in the composable.
 *
 * [isAdmin] controls whether the delete icon is shown. It is populated by
 * fetching the current user's claims from GET /api/v1/identity.
 */
data class RequestsUiState(
    val movieRequests:  List<MovieRequest> = emptyList(),
    val tvRequests:     List<TvRequest>    = emptyList(),
    val selectedTab:    RequestTab         = RequestTab.MOVIES,
    val selectedStatus: StatusTab          = StatusTab.PENDING,
    val isLoading:      Boolean            = false,
    val error:          String?            = null,
    val isAdmin:        Boolean            = false
) {
    // Pending = not yet available AND not denied
    val pendingMovies:   List<MovieRequest> get() = movieRequests.filter { !it.available && it.denied != true }
    // Processed = available OR denied
    val processedMovies: List<MovieRequest> get() = movieRequests.filter { it.available || it.denied == true }

    val pendingTv:   List<TvRequest> get() = tvRequests.filter { !it.available && it.denied != true }
    val processedTv: List<TvRequest> get() = tvRequests.filter { it.available || it.denied == true }

    /** The list currently visible given the active [selectedTab] and [selectedStatus]. */
    val visibleMovies: List<MovieRequest> get() = if (selectedStatus == StatusTab.PENDING) pendingMovies else processedMovies
    val visibleTv:     List<TvRequest>    get() = if (selectedStatus == StatusTab.PENDING) pendingTv else processedTv
}

/**
 * ViewModel for the Requests screen.
 *
 * On load, movie requests, TV requests, and the current user profile are all
 * fetched concurrently. The user profile is needed to determine [RequestsUiState.isAdmin]
 * so the delete button can be shown or hidden without a separate network call.
 */
@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val repository: OmbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestsUiState(isLoading = true))
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Refreshes all request lists and re-checks admin status. */
    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Fan-out: all three requests run in parallel
            val movies = async { repository.getMovieRequests() }
            val tv     = async { repository.getTvRequests() }
            val user   = async { repository.getCurrentUser() }

            _uiState.value = _uiState.value.copy(
                movieRequests = movies.await().getOrDefault(
                    com.ombi.mobile.data.api.models.RequestsViewModel(emptyList(), 0)
                ).collection,
                tvRequests = tv.await().getOrDefault(
                    com.ombi.mobile.data.api.models.RequestsViewModel(emptyList(), 0)
                ).collection,
                isAdmin   = user.await().getOrNull()?.isAdmin ?: false,
                isLoading = false
            )
        }
    }

    /** Switches the primary tab between Movies and TV. */
    fun onTabSelected(tab: RequestTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    /** Switches the secondary tab between Pending and Processed. */
    fun onStatusSelected(status: StatusTab) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
    }

    /**
     * Deletes a movie request by its ID and removes it from the local list on success.
     * Only admin users can delete requests — the Ombi backend enforces this too.
     */
    fun cancelMovieRequest(requestId: Int) {
        viewModelScope.launch {
            repository.cancelMovieRequest(requestId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        movieRequests = _uiState.value.movieRequests.filter { it.id != requestId }
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    /**
     * Deletes a TV parent request by its parent ID and removes all associated child
     * requests (seasons) from the local list on success.
     *
     * @param parentRequestId The [com.ombi.mobile.data.api.models.TvParentRequest.id],
     *   NOT the child request's own id. The Ombi DELETE endpoint expects the parent ID.
     */
    fun cancelTvRequest(parentRequestId: Int) {
        viewModelScope.launch {
            repository.cancelTvRequest(parentRequestId)
                .onSuccess {
                    // Remove all child requests belonging to this parent
                    _uiState.value = _uiState.value.copy(
                        tvRequests = _uiState.value.tvRequests.filter {
                            it.parentRequest?.id != parentRequestId
                        }
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }
}
