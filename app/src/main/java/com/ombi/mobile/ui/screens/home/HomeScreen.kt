package com.ombi.mobile.ui.screens.home

import androidx.compose.foundation.layout.*
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
                        uiState.recentMovies.take(20).forEach { movie ->
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
                        uiState.recentTv.take(20).forEach { tv ->
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
                        uiState.popularMovies.take(20).forEach { movie ->
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
                        uiState.trendingTv.take(20).forEach { tv ->
                            MediaCard(
                                title = tv.title,
                                posterUrl = tv.backdropPath.toTmdbUrl(),
                                onClick = { viewModel.selectItem(tv.toMediaItem()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.upcomingMovies.isNotEmpty()) {
                    MediaRow(title = "Upcoming Movies") {
                        uiState.upcomingMovies.take(20).forEach { movie ->
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
