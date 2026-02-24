package com.ombi.mobile.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ombi.mobile.ui.components.MediaCard
import com.ombi.mobile.ui.components.toTmdbUrl
import com.ombi.mobile.ui.model.toMediaItem
import com.ombi.mobile.ui.screens.detail.MediaDetailSheet

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            isLoading = uiState.isLoading
        )

        FilterRow(
            selected = uiState.filter,
            onSelect = viewModel::onFilterChange
        )

        when {
            uiState.query.length < 2 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Search for movies and TV shows",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            uiState.filteredResults.isEmpty() && !uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredResults, key = { "${it.mediaType}_${it.id}" }) { result ->
                        MediaCard(
                            title = result.title,
                            posterUrl = result.poster.toTmdbUrl(),
                            onClick = { viewModel.selectItem(result.toMediaItem()) }
                        )
                    }
                }
            }
        }

        uiState.error?.let {
            Snackbar(modifier = Modifier.padding(8.dp)) { Text(it) }
        }
    }

    // Detail bottom sheet
    uiState.selectedItem?.let { item ->
        MediaDetailSheet(
            item = item,
            onDismiss = { viewModel.selectItem(null) },
            onRequest = viewModel::requestSelected,
            isRequesting = uiState.isRequesting,
            requestMessage = uiState.requestMessage
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, isLoading: Boolean) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search movies & TV shows…") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun FilterRow(selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        SearchFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
