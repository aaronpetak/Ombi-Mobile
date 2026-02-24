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

data class HomeUiState(
    val recentMovies: List<RecentlyAddedMovie> = emptyList(),
    val recentTv: List<RecentlyAddedTv> = emptyList(),
    val popularMovies: List<SearchMovieViewModel> = emptyList(),
    val trendingTv: List<SearchTvShowViewModel> = emptyList(),
    val upcomingMovies: List<SearchMovieViewModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedItem: MediaItem? = null,
    val isRequesting: Boolean = false,
    val requestMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OmbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val recentMovies = async { repository.getRecentlyAddedMovies() }
            val recentTv = async { repository.getRecentlyAddedTv() }
            val popularMovies = async { repository.getPopularMovies() }
            val trendingTv = async { repository.getTrendingTv() }
            val upcomingMovies = async { repository.getUpcomingMovies() }

            _uiState.value = _uiState.value.copy(
                recentMovies = recentMovies.await().getOrDefault(emptyList()),
                recentTv = recentTv.await().getOrDefault(emptyList()),
                popularMovies = popularMovies.await().getOrDefault(emptyList()),
                trendingTv = trendingTv.await().getOrDefault(emptyList()),
                upcomingMovies = upcomingMovies.await().getOrDefault(emptyList()),
                isLoading = false
            )
        }
    }

    fun selectItem(item: MediaItem?) {
        _uiState.value = _uiState.value.copy(selectedItem = item, requestMessage = null)
    }

    fun requestSelected() {
        val item = _uiState.value.selectedItem ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRequesting = true, requestMessage = null)
            val result = if (item.isMovie) {
                item.theMovieDbId?.let { repository.requestMovie(it) }
            } else {
                // theMovieDbId is required for V2 TV requests; discover endpoints may only
                // populate tvDbId, so resolve the TMDB ID via a TVDB lookup if needed
                val tmdbId = item.theMovieDbId
                    ?: item.tvDbId?.let { repository.getTvByTvDbId(it).getOrNull()?.theMovieDbId }
                tmdbId?.let { repository.requestTv(it) }
            }
            result?.fold(
                onSuccess = { engineResult ->
                    val msg = if (engineResult.result) "Request submitted!" else "Failed: ${engineResult.errorMessage}"
                    _uiState.value = _uiState.value.copy(
                        isRequesting = false,
                        requestMessage = msg,
                        selectedItem = if (engineResult.result) item.copy(requested = true) else item
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isRequesting = false,
                        requestMessage = "Error: ${it.message}"
                    )
                }
            ) ?: run {
                _uiState.value = _uiState.value.copy(isRequesting = false, requestMessage = "Missing ID for request")
            }
        }
    }
}
