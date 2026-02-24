package com.ombi.mobile.ui.screens.requests

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ombi.mobile.data.api.models.MovieRequest
import com.ombi.mobile.data.api.models.TvRequest
import com.ombi.mobile.ui.components.toTmdbUrl

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

        // Primary tab row: Movies | TV
        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            RequestTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.onTabSelected(tab) },
                    text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        // Secondary tab row: Pending | Processed
        SecondaryTabRow(selectedTabIndex = uiState.selectedStatus.ordinal) {
            StatusTab.entries.forEach { status ->
                Tab(
                    selected = uiState.selectedStatus == status,
                    onClick = { viewModel.onStatusSelected(status) },
                    text = {
                        val label = if (status == StatusTab.PENDING) "Pending" else "Processed"
                        val count = when {
                            status == StatusTab.PENDING && uiState.selectedTab == RequestTab.MOVIES -> uiState.pendingMovies.size
                            status == StatusTab.PROCESSED && uiState.selectedTab == RequestTab.MOVIES -> uiState.processedMovies.size
                            status == StatusTab.PENDING -> uiState.pendingTv.size
                            else -> uiState.processedTv.size
                        }
                        Text("$label ($count)")
                    }
                )
            }
        }

        val canCancel = uiState.selectedStatus == StatusTab.PENDING

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::load
        ) {
            when (uiState.selectedTab) {
                RequestTab.MOVIES -> RequestList(
                    isLoading = uiState.isLoading,
                    emptyMessage = if (canCancel) "No pending movie requests" else "No completed movie requests",
                    items = uiState.visibleMovies,
                    key = { it.id },
                    title = { it.title ?: "Unknown" },
                    posterPath = { it.posterPath },
                    statusLabel = { it.statusLabel },
                    canCancel = canCancel,
                    onCancel = { viewModel.cancelMovieRequest(it.id) }
                )
                RequestTab.TV -> RequestList(
                    isLoading = uiState.isLoading,
                    emptyMessage = if (canCancel) "No pending TV requests" else "No completed TV requests",
                    items = uiState.visibleTv,
                    key = { it.id },
                    title = { it.title ?: "Unknown" },
                    posterPath = { it.posterPath ?: it.background },
                    statusLabel = { tvStatusLabel(it) },
                    canCancel = canCancel,
                    onCancel = { viewModel.cancelTvRequest(it.id) }
                )
            }
        }
    }
}

private fun tvStatusLabel(request: TvRequest): String = when {
    request.available -> "Available"
    request.denied == true -> "Denied"
    request.childRequests?.any { it.approved } == true -> "Processing"
    else -> "Pending"
}

@Composable
private fun <T> RequestList(
    isLoading: Boolean,
    emptyMessage: String,
    items: List<T>,
    key: (T) -> Any,
    title: (T) -> String,
    posterPath: (T) -> String?,
    statusLabel: (T) -> String,
    canCancel: Boolean,
    onCancel: (T) -> Unit
) {
    if (isLoading && items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(items, key = { key(it) }) { item ->
            RequestListItem(
                title = title(item),
                posterPath = posterPath(item),
                statusLabel = statusLabel(item),
                canCancel = canCancel,
                onCancel = { onCancel(item) }
            )
        }
    }
}

@Composable
private fun RequestListItem(
    title: String,
    posterPath: String?,
    statusLabel: String,
    canCancel: Boolean,
    onCancel: () -> Unit
) {
    val statusColor: Color = when (statusLabel) {
        "Available"  -> Color(0xFF388E3C)
        "Denied"     -> MaterialTheme.colorScheme.error
        "Processing" -> Color(0xFFF57C00)
        else         -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(statusLabel, color = statusColor)
        },
        leadingContent = {
            AsyncImage(
                model = posterPath.toTmdbUrl("w92"),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        },
        trailingContent = {
            if (canCancel) {
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
