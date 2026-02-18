package family.petak.ombi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class MediaStatus {
    Available, Requested, Approved, Pending, Denied, Unknown
}

@Composable
fun StatusBadge(status: MediaStatus) {
    val (label, color) = when (status) {
        MediaStatus.Available -> "Available" to Color(0xFF388E3C)
        MediaStatus.Requested -> "Requested" to Color(0xFF1976D2)
        MediaStatus.Approved -> "Processing" to Color(0xFFF57C00)
        MediaStatus.Pending -> "Pending" to Color(0xFF616161)
        MediaStatus.Denied -> "Denied" to Color(0xFFD32F2F)
        MediaStatus.Unknown -> return
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

fun mediaStatusFrom(available: Boolean, requested: Boolean, approved: Boolean, denied: Boolean?): MediaStatus =
    when {
        available -> MediaStatus.Available
        denied == true -> MediaStatus.Denied
        approved -> MediaStatus.Approved
        requested -> MediaStatus.Requested
        else -> MediaStatus.Unknown
    }
