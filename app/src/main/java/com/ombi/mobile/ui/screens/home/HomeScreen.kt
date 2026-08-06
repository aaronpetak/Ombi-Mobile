package com.ombi.mobile.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ombi.mobile.ui.components.MediaCard
import com.ombi.mobile.ui.components.MediaRow
import com.ombi.mobile.ui.components.toTmdbUrl
import com.ombi.mobile.ui.model.*
import com.ombi.mobile.ui.screens.detail.MediaDetailSheet

/**
 * Home / Discover screen showing curated content rows fetched from the Ombi instance.
 *
 * Content rows (each capped at 20 items, hidden when empty):
 * - Recently Added Movies
 * - Recently Added TV
 * - Popular Movies
 * - Trending TV
 * - Upcoming Movies
 *
 * Each row is a horizontally-scrollable [MediaRow] of [MediaCard]s.
 * Tapping a card opens a [MediaDetailSheet] where the user can request the media.
 *
 * Pull-to-refresh triggers [HomeViewModel.load], which re-fetches all rows concurrently.
 * A full-screen spinner is shown only on the very first load when all rows are empty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = viewModel::load
    ) {
        if (uiState.isLoading && uiState.recentMovies.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Home",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (uiState.recentMovies.isNotEmpty()) {
                    MediaRow(title = "Recently Added Movies") {
                        items(uiState.recentMovies.take(20), key = { it.id }) { movie ->
                            MediaCard(
                                title = movie.title,
                                posterUrl = movie.posterPath.toTmdbUrl(),
                                onClick = { viewModel.selectItem(movie.toMediaItem()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.recentTv.isNotEmpty()) {
                    MediaRow(title = "Recently Added TV") {
                        items(uiState.recentTv.take(20), key = { it.id }) { tv ->
                            MediaCard(
                                title = tv.title,
                                posterUrl = tv.posterPath.toTmdbUrl(),
                                onClick = { viewModel.selectItem(tv.toMediaItem()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.popularMovies.isNotEmpty()) {
                    MediaRow(title = "Popular Movies") {
                        items(uiState.popularMovies.take(20), key = { it.id }) { movie ->
                            MediaCard(
                                title = movie.title,
                                posterUrl = movie.posterPath.toTmdbUrl(),
                                onClick = { viewModel.selectItem(movie.toMediaItem()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.trendingTv.isNotEmpty()) {
                    MediaRow(title = "Trending TV") {
                        items(uiState.trendingTv.take(20), key = { it.id }) { tv ->
                            MediaCard(
                                title = tv.title,
                                // Prefer the portrait poster; fall back to the landscape
                                // backdrop only if the list response omits it.
                                posterUrl = (tv.posterPath ?: tv.backdropPath).toTmdbUrl(),
                                onClick = { viewModel.selectItem(tv.toMediaItem()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.upcomingMovies.isNotEmpty()) {
                    MediaRow(title = "Upcoming Movies") {
                        items(uiState.upcomingMovies.take(20), key = { it.id }) { movie ->
                            MediaCard(
                                title = movie.title,
                                posterUrl = movie.posterPath.toTmdbUrl(),
                                onClick = { viewModel.selectItem(movie.toMediaItem()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
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
