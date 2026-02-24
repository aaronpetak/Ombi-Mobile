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

enum class RequestTab { MOVIES, TV }
enum class StatusTab { PENDING, PROCESSED }

data class RequestsUiState(
    val movieRequests: List<MovieRequest> = emptyList(),
    val tvRequests: List<TvRequest> = emptyList(),
    val selectedTab: RequestTab = RequestTab.MOVIES,
    val selectedStatus: StatusTab = StatusTab.PENDING,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // Pending = not yet available and not denied
    val pendingMovies: List<MovieRequest> get() = movieRequests.filter { !it.available && it.denied != true }
    // Processed = available or denied
    val processedMovies: List<MovieRequest> get() = movieRequests.filter { it.available || it.denied == true }

    val pendingTv: List<TvRequest> get() = tvRequests.filter { !it.available && it.denied != true }
    val processedTv: List<TvRequest> get() = tvRequests.filter { it.available || it.denied == true }

    val visibleMovies: List<MovieRequest> get() = if (selectedStatus == StatusTab.PENDING) pendingMovies else processedMovies
    val visibleTv: List<TvRequest> get() = if (selectedStatus == StatusTab.PENDING) pendingTv else processedTv
}

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val repository: OmbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestsUiState(isLoading = true))
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val movies = async { repository.getMovieRequests() }
            val tv = async { repository.getTvRequests() }
            _uiState.value = _uiState.value.copy(
                movieRequests = movies.await().getOrDefault(
                    com.ombi.mobile.data.api.models.RequestsViewModel(emptyList(), 0)
                ).collection,
                tvRequests = tv.await().getOrDefault(
                    com.ombi.mobile.data.api.models.RequestsViewModel(emptyList(), 0)
                ).collection,
                isLoading = false
            )
        }
    }

    fun onTabSelected(tab: RequestTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun onStatusSelected(status: StatusTab) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
    }

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

    fun cancelTvRequest(requestId: Int) {
        viewModelScope.launch {
            repository.cancelTvRequest(requestId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        tvRequests = _uiState.value.tvRequests.filter { it.id != requestId }
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }
}
