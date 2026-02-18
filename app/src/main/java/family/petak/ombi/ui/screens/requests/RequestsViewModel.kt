package family.petak.ombi.ui.screens.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import family.petak.ombi.data.api.models.MovieRequest
import family.petak.ombi.data.api.models.TvRequest
import family.petak.ombi.data.repository.OmbiRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RequestTab { MOVIES, TV }

data class RequestsUiState(
    val movieRequests: List<MovieRequest> = emptyList(),
    val tvRequests: List<TvRequest> = emptyList(),
    val selectedTab: RequestTab = RequestTab.MOVIES,
    val isLoading: Boolean = false,
    val error: String? = null
)

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
                    family.petak.ombi.data.api.models.RequestsViewModel(emptyList(), 0)
                ).collection,
                tvRequests = tv.await().getOrDefault(
                    family.petak.ombi.data.api.models.RequestsViewModel(emptyList(), 0)
                ).collection,
                isLoading = false
            )
        }
    }

    fun onTabSelected(tab: RequestTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
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
