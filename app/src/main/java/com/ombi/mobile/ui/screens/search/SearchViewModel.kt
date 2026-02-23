package com.ombi.mobile.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.api.models.MultiSearchResult
import com.ombi.mobile.data.repository.OmbiRepository
import com.ombi.mobile.ui.model.MediaItem
import com.ombi.mobile.ui.model.toMediaItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchFilter { ALL, MOVIES, TV }

data class SearchUiState(
    val query: String = "",
    val results: List<MultiSearchResult> = emptyList(),
    val filter: SearchFilter = SearchFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedItem: MediaItem? = null,
    val isRequesting: Boolean = false,
    val requestMessage: String? = null
) {
    val filteredResults: List<MultiSearchResult> get() = when (filter) {
        SearchFilter.ALL -> results
        SearchFilter.MOVIES -> results.filter { it.isMovie }
        SearchFilter.TV -> results.filter { it.isTv }
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: OmbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(400)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collectLatest { query -> performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null)
        queryFlow.value = query
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList())
        }
    }

    fun onFilterChange(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
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
                item.tvDbId?.let { repository.requestTv(it) }
            }
            result?.fold(
                onSuccess = { engineResult ->
                    val msg = if (engineResult.result) "Request submitted!" else "Failed: ${engineResult.errorMessage}"
                    _uiState.value = _uiState.value.copy(
                        isRequesting = false,
                        requestMessage = msg,
                        selectedItem = if (engineResult.result) item.copy(requested = true) else item,
                        // Refresh status badge in the grid
                        results = if (engineResult.result) {
                            _uiState.value.results.map { r ->
                                if ((item.isMovie && r.theMovieDbId == item.theMovieDbId) ||
                                    (!item.isMovie && r.tvDbId == item.tvDbId)
                                ) r.copy(requested = true) else r
                            }
                        } else _uiState.value.results
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

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.multiSearch(query)
            .onSuccess { _uiState.value = _uiState.value.copy(results = it, isLoading = false) }
            .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoading = false) }
    }
}
