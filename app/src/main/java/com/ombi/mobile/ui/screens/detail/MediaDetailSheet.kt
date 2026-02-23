package com.ombi.mobile.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ombi.mobile.ui.components.toTmdbUrl
import com.ombi.mobile.ui.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailSheet(
    item: MediaItem,
    onDismiss: () -> Unit,
    onRequest: () -> Unit,
    isRequesting: Boolean = false,
    requestMessage: String? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Poster + core info
            Row(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.posterPath.toTmdbUrl("w342"),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.year?.let {
                            AssistChip(onClick = {}, label = { Text(it) })
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text(if (item.isMovie) "Movie" else "TV Show") }
                        )
                    }

                    item.rating?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "★ ${"%.1f".format(it)} / 10",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    StatusChip(item)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Overview
            if (!item.overview.isNullOrBlank()) {
                Text("Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(item.overview, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }

            // Request button
            val canRequest = !item.available && !item.requested && !item.approved && item.denied == false

            Button(
                onClick = onRequest,
                enabled = canRequest && !isRequesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    isRequesting -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    item.available -> Text("Available")
                    item.requested || item.approved -> Text("Already Requested")
                    else -> Text(if (item.isMovie) "Request Movie" else "Request TV Show")
                }
            }

            requestMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Error") || it.startsWith("Failed"))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(item: MediaItem) {
    val (label, color) = when {
        item.available -> "Available" to Color(0xFF388E3C)
        item.denied -> "Denied" to Color(0xFFD32F2F)
        item.approved -> "Processing" to Color(0xFFF57C00)
        item.requested -> "Requested" to Color(0xFF1976D2)
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
