package com.ombi.mobile.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
 * Builds a full TMDB image URL from a relative poster path returned by Ombi.
 * Handles null, already-full URLs, and paths missing the leading '/'.
 */
fun String?.toTmdbUrl(size: String = "w342"): String? {
    if (isNullOrBlank()) return null
    if (startsWith("http")) return this
    val path = if (startsWith("/")) this else "/$this"
    return "https://image.tmdb.org/t/p/$size$path"
}

/**
 * Compact poster card used in content rows and the search grid.
 *
 * Displays a poster image at a 2:3 aspect ratio with an optional [statusBadge]
 * overlaid in the top-right corner, and up to two lines of title text below.
 *
 * @param title Display title shown below the poster; omitted if null or blank.
 * @param posterUrl Full TMDB image URL (use [toTmdbUrl] to build from a relative path).
 * @param modifier Additional layout modifiers.
 * @param width Fixed card width; defaults to 120 dp.
 * @param statusBadge Optional composable overlaid on the top-right of the poster.
 * @param onClick Tap callback; card is non-interactive if null.
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

/** Labeled horizontally-scrollable row of [MediaCard]s. */
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
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            content = content
        )
    }
}
