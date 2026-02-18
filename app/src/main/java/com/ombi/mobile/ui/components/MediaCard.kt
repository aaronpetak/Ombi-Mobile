package com.ombi.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Poster card used throughout Home, Search, and Requests screens.
 *
 * @param posterUrl  Full URL for the poster image (Ombi returns relative paths — callers must
 *                   prepend the TMDB image base URL, e.g. https://image.tmdb.org/t/p/w342)
 */
@Composable
fun MediaCard(
    title: String?,
    posterUrl: String?,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    statusBadge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.width(width),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick ?: {}
    ) {
        Box {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            statusBadge?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) { it() }
            }
        }

        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

/** Horizontal scrollable row of [MediaCard]s with a section header. */
@Composable
fun MediaRow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            content = content
        )
    }
}
