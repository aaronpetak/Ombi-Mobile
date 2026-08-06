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
import com.ombi.mobile.data.api.models.RequestStatus
import com.ombi.mobile.data.api.models.TvRequest
import com.ombi.mobile.ui.components.toDisplayLabel
import com.ombi.mobile.ui.components.toTmdbUrl

/**
 * Requests screen showing the current user's movie and TV requests.
 *
 * Layout:
 * - Primary [TabRow]: Movies / TV
 * - Secondary [SecondaryTabRow]: Pending / Processed (with live counts)
 * - [PullToRefreshBox] wrapping a generic [RequestList]
 *
 * The delete icon is only shown when [RequestsUiState.isAdmin] is true AND the
 * Pending tab is selected — admin-only, can't undo a fulfilled request anyway.
 *
 * TV cancellation passes the parent request ID (not the child/season ID) because
 * the Ombi DELETE endpoint expects the parent. See [RequestsViewModel.cancelTvRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(viewModel: RequestsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Memoize the pending/processed splits so the filtering runs only when the
    // underlying request lists change, not on every recomposition (the secondary
    // tab labels and the list body would otherwise each re-filter every frame).
    // The predicate stays single-sourced on RequestsUiState (and unit-tested);
    // remember only caches the read.
    val pendingMovies   = remember(uiState.movieRequests) { uiState.pendingMovies }
    val processedMovies = remember(uiState.movieRequests) { uiState.processedMovies }
    val pendingTv       = remember(uiState.tvRequests)    { uiState.pendingTv }
    val processedTv     = remember(uiState.tvRequests)    { uiState.processedTv }
    val visibleMovies   = if (uiState.selectedStatus == StatusTab.PENDING) pendingMovies else processedMovies
    val visibleTv       = if (uiState.selectedStatus == StatusTab.PENDING) pendingTv else processedTv

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
                    text = { Text(tab.toDisplayLabel()) }
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
                        val label = when (status) {
                            StatusTab.PENDING -> "Pending"
                            StatusTab.PROCESSED -> "Processed"
                        }
                        val count = when (status) {
                            StatusTab.PENDING -> if (uiState.selectedTab == RequestTab.MOVIES) pendingMovies.size else pendingTv.size
                            StatusTab.PROCESSED -> if (uiState.selectedTab == RequestTab.MOVIES) processedMovies.size else processedTv.size
                        }
                        Text("$label ($count)")
                    }
                )
            }
        }

        val canCancel = uiState.selectedStatus == StatusTab.PENDING && uiState.isAdmin

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::load
        ) {
            when (uiState.selectedTab) {
                RequestTab.MOVIES -> RequestList(
                    isLoading = uiState.isLoading,
                    emptyMessage = if (canCancel) "No pending movie requests" else "No completed movie requests",
                    items = visibleMovies,
                    key = { it.id },
                    title = { it.title ?: "Unknown" },
                    posterPath = { it.posterPath },
                    status = { it.status },
                    canCancel = { canCancel },
                    onCancel = { viewModel.cancelMovieRequest(it.id) }
                )
                RequestTab.TV -> RequestList(
                    isLoading = uiState.isLoading,
                    emptyMessage = if (canCancel) "No pending TV requests" else "No completed TV requests",
                    items = visibleTv,
                    key = { it.id },
                    title = { it.title ?: it.parentRequest?.title ?: "Unknown" },
                    posterPath = { it.posterPath },
                    status = { it.status },
                    // Cancellation targets the parent request ID; without it the DELETE
                    // would hit the wrong (child) ID, so hide the button in that case.
                    canCancel = { canCancel && it.parentRequest?.id != null },
                    onCancel = { request ->
                        request.parentRequest?.id?.let { parentId -> viewModel.cancelTvRequest(parentId) }
                    }
                )
            }
        }
    }
}

/**
 * Generic, type-safe list of request items.
 *
 * Handles three states: initial loading spinner, empty-state message, and the
 * scrollable list. Typed with [T] so the same composable serves both
 * [MovieRequest] and [TvRequest] without duplication.
 *
 * @param isLoading True while the first load is in progress.
 * @param emptyMessage Message to display when [items] is empty and not loading.
 * @param items The visible request list (already filtered by the active tab/status).
 * @param key Stable identity key for [LazyColumn] item reuse.
 * @param title Extracts the display title from an item.
 * @param posterPath Extracts the relative TMDB poster path from an item.
 * @param status Extracts the [RequestStatus] from an item.
 * @param canCancel Per-item predicate: whether to show the delete icon for a
 *   given item (admin + pending tab, and for TV a non-null parent request).
 * @param onCancel Callback invoked when the delete icon is tapped.
 */
@Composable
private fun <T> RequestList(
    isLoading: Boolean,
    emptyMessage: String,
    items: List<T>,
    key: (T) -> Any,
    title: (T) -> String,
    posterPath: (T) -> String?,
    status: (T) -> RequestStatus,
    canCancel: (T) -> Boolean,
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
                status = status(item),
                canCancel = canCancel(item),
                onCancel = { onCancel(item) }
            )
        }
    }
}

/**
 * A single row in the request list.
 *
 * Shows a small poster thumbnail, title, colour-coded status label, and — when
 * [canCancel] is true — a red delete icon button at the trailing edge.
 *
 * Status colours:
 * - Green   → Available
 * - Red     → Denied
 * - Orange  → Processing (approved but not yet available)
 * - Grey    → Pending (default)
 */
@Composable
private fun RequestListItem(
    title: String,
    posterPath: String?,
    status: RequestStatus,
    canCancel: Boolean,
    onCancel: () -> Unit
) {
    // Exhaustive over RequestStatus — adding a status is now a compile error
    // here until a colour is chosen, rather than silently falling through.
    val statusColor: Color = when (status) {
        RequestStatus.AVAILABLE  -> Color(0xFF388E3C)
        RequestStatus.DENIED     -> MaterialTheme.colorScheme.error
        RequestStatus.PROCESSING -> Color(0xFFF57C00)
        RequestStatus.PENDING    -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(status.label, color = statusColor)
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
