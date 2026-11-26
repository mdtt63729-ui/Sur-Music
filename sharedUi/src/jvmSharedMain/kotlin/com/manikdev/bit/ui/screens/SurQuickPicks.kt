package com.manikdev.bit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.manikdev.bit.data.model.ShelfItem

/**
 * Quick picks, ported from the Sur repo's `QuickPicks` in `HomeScreen.kt`.
 *
 * Upstream draws it as a **four-row horizontal grid** rather than a single row
 * of cards: 224dp tall on Android (56dp a row, where Home packs tighter than
 * Desktop's 64dp), each cell a 56dp sleeve beside the track and its artist, and
 * the header carries YouTube's "Let's start with a radio" line above
 * "Quick picks".
 *
 * The grid scrolls sideways and snaps, so a swipe lands a whole column at the
 * start edge rather than halfway between two.
 */
@Composable
fun SurQuickPicks(
    items: List<ShelfItem>,
    onItemClick: (ShelfItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 15.dp)) {
            Text(
                text = "Let's start with a radio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Quick picks",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 15.dp,
                vertical = 4.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items, key = { it.videoId ?: it.browseId ?: it.title }) { item ->
                SurQuickPickItem(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

/** One quick pick: a 56dp sleeve, the track and its artist beside it. */
@Composable
private fun SurQuickPickItem(
    item: ShelfItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
