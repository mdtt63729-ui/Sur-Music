package com.manikdev.bit.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Home chip strip, ported from the Sur repo's `ChipGroup.kt` and the row
 * that draws it in `HomeScreen.kt`.
 *
 * Upstream's chip: an [FilterChip] on a circle, no elevation, **no fill at all
 * while unselected** — the outline is what carries the shape, because these sit
 * over artwork and a solid pill reads as a slab punched into the picture — and a
 * solid `primary` fill once selected, with `onPrimary` for its label. No tick
 * icon: the fill is already the contrast, and the icon reflowed the row under
 * the finger that had just tapped it.
 *
 * The strip is a plain scrolling Row, 56dp tall, chips 4dp apart, with each end
 * faded out while more chips wait past it.
 */
@Composable
fun SurChipStrip(
    chips: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .chipStripEdgeFades(32.dp)
            .horizontalScroll(state),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            chips.forEachIndexed { index, label ->
                SurChip(
                    text = label,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

/**
 * One chip: [FilterChip] on a circle with the fill and rim upstream uses.
 */
@Composable
fun SurChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    FilterChip(
        modifier = modifier,
        shape = CircleShape,
        // No shadow: an unfilled chip over artwork shows a drop shadow through
        // itself as a dark ring rather than behind it.
        elevation = FilterChipDefaults.filterChipElevation(elevation = 0.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            labelColor = labelColor,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        onClick = onClick,
        label = {
            Text(text, maxLines = 1, style = LocalTextStyle.current)
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = Color.Transparent,
            borderColor = borderColor,
        ),
        selected = selected,
    )
}

/**
 * Spotify's collapsing filter, ported from upstream's `ExpandableChip`: closed
 * it is one chip; tapped it opens into its options inside the same pill; once
 * one is picked it folds back into one pill reading "Mood | option", the head
 * half a brighter capsule.
 */
@Composable
fun SurExpandableChip(
    text: String,
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val picked = selectedIndex?.let(options::getOrNull)
    var lastPicked by remember { mutableStateOf(picked) }
    if (picked != null) lastPicked = picked
    val pickedLabel = picked ?: lastPicked.orEmpty()
    val active = expanded || picked != null
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, if (picked != null) Color.Transparent else colorScheme.outlineVariant, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SurChip(
            text = text,
            selected = active,
            containerColor = if (active) colorScheme.primary else Color.Transparent,
            labelColor = if (active) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            borderColor = Color.Transparent,
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            options.forEachIndexed { index, option ->
                SurChip(
                    text = option,
                    selected = index == selectedIndex,
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else if (pickedLabel.isNotEmpty()) {
            SurChip(
                text = pickedLabel,
                selected = true,
                containerColor = colorScheme.primary,
                labelColor = colorScheme.onPrimary,
                borderColor = Color.Transparent,
                onClick = { expanded = true },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/**
 * Upstream `chipStripEdgeFades`: each end of the strip dissolves while chips
 * wait past it, drawn as a DstIn mask over an offscreen layer.
 */
fun Modifier.chipStripEdgeFades(width: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = width.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                1f to Color.Black,
                startX = 0f,
                endX = fade,
            ),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Black,
                1f to Color.Transparent,
                startX = size.width - fade,
                endX = size.width,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

/** The strip's chips: All, then the moods upstream folds into one pill. */
@Composable
fun surHomeChipLabels(all: String, mood: String): Pair<String, String> = all to mood
