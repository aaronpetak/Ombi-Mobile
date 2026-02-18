package family.petak.ombi.ui.screens.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import family.petak.ombi.data.api.models.MovieRequest
import family.petak.ombi.data.api.models.TvRequest

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w92"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(viewModel: RequestsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "My Requests",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Tab row
        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            RequestTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.onTabSelected(tab) },
                    text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::load
        ) {
            when (uiState.selectedTab) {
                RequestTab.MOVIES -> MovieRequestsList(
                    requests = uiState.movieRequests,
                    isLoading = uiState.isLoading,
                    onCancel = viewModel::cancelMovieRequest
                )
                RequestTab.TV -> TvRequestsList(
                    requests = uiState.tvRequests,
                    isLoading = uiState.isLoading,
                    onCancel = viewModel::cancelTvRequest
                )
            }
        }
    }
}

@Composable
private fun MovieRequestsList(
    requests: List<MovieRequest>,
    isLoading: Boolean,
    onCancel: (Int) -> Unit
) {
    if (isLoading && requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No movie requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(requests, key = { it.id }) { request ->
            RequestListItem(
                title = request.title ?: "Unknown",
                posterPath = request.posterPath,
                statusLabel = request.statusLabel,
                isPending = !request.approved && request.denied != true && !request.available,
                onCancel = { onCancel(request.id) }
            )
        }
    }
}

@Composable
private fun TvRequestsList(
    requests: List<TvRequest>,
    isLoading: Boolean,
    onCancel: (Int) -> Unit
) {
    if (isLoading && requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No TV requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(requests, key = { it.id }) { request ->
            val isPending = request.childRequests?.any {
                !it.approved && it.denied != true && !it.available
            } == true
            RequestListItem(
                title = request.title ?: "Unknown",
                posterPath = request.posterPath,
                statusLabel = if (request.available) "Available" else if (isPending) "Pending" else "Processing",
                isPending = isPending,
                onCancel = { onCancel(request.id) }
            )
        }
    }
}

@Composable
private fun RequestListItem(
    title: String,
    posterPath: String?,
    statusLabel: String,
    isPending: Boolean,
    onCancel: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                statusLabel,
                color = when (statusLabel) {
                    "Available" -> MaterialTheme.colorScheme.tertiary
                    "Denied" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        leadingContent = {
            AsyncImage(
                model = posterPath?.let { TMDB_IMAGE_BASE + it },
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        },
        trailingContent = {
            if (isPending) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Cancel request",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
    HorizontalDivider()
}
