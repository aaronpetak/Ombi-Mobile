package com.ombi.mobile.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ombi.mobile.data.api.models.MultiSearchResult
import com.ombi.mobile.data.repository.OmbiRepository
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
    val error: String? = null
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

    // Debounce search input to avoid firing on every keystroke
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

    fun requestMovie(movieDbId: Int) {
        viewModelScope.launch {
            repository.requestMovie(movieDbId)
                .onSuccess {
                    // Optimistically mark as requested in the results list
                    _uiState.value = _uiState.value.copy(
                        results = _uiState.value.results.map { result ->
                            if (result.theMovieDbId == movieDbId) result.copy(requested = true) else result
                        }
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun requestTv(tvDbId: Int) {
        viewModelScope.launch {
            repository.requestTv(tvDbId, requestAll = true)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        results = _uiState.value.results.map { result ->
                            if (result.tvDbId == tvDbId) result.copy(requested = true) else result
                        }
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.multiSearch(query)
            .onSuccess { _uiState.value = _uiState.value.copy(results = it, isLoading = false) }
            .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoading = false) }
    }
}
