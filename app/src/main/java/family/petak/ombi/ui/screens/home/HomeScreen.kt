package family.petak.ombi.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import family.petak.ombi.ui.components.MediaCard
import family.petak.ombi.ui.components.MediaRow

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w342"

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
                        uiState.recentMovies.take(10).forEach { movie ->
                            MediaCard(
                                title = movie.title,
                                posterUrl = movie.posterPath?.let { TMDB_IMAGE_BASE + it }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.recentTv.isNotEmpty()) {
                    MediaRow(title = "Recently Added TV") {
                        uiState.recentTv.take(10).forEach { tv ->
                            MediaCard(
                                title = tv.title,
                                posterUrl = tv.posterPath?.let { TMDB_IMAGE_BASE + it }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.popularMovies.isNotEmpty()) {
                    MediaRow(title = "Popular Movies") {
                        uiState.popularMovies.take(10).forEach { movie ->
                            MediaCard(
                                title = movie.title,
                                posterUrl = movie.poster?.let { TMDB_IMAGE_BASE + it }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.trendingTv.isNotEmpty()) {
                    MediaRow(title = "Trending TV") {
                        uiState.trendingTv.take(10).forEach { tv ->
                            MediaCard(
                                title = tv.title,
                                posterUrl = tv.poster?.let { TMDB_IMAGE_BASE + it }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.upcomingMovies.isNotEmpty()) {
                    MediaRow(title = "Upcoming Movies") {
                        uiState.upcomingMovies.take(10).forEach { movie ->
                            MediaCard(
                                title = movie.title,
                                posterUrl = movie.poster?.let { TMDB_IMAGE_BASE + it }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
