package com.manikdev.bit.ui.player.m3e

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Connected button group: six 48dp slots with 3dp gaps and rounded end caps —
 * Info | Output | Shuffle | Repeat | Add-to-playlist | Queue.
 *
 * Ported line for line from limusic-android's `ExpressiveConnectedGroup`
 * (`docs/player-port/limusic/NowPlayingContentM3Expressive.kt.txt`), with its
 * `LimusicIcons` swapped for the Material set, its `RepeatState` for this app's
 * repeat mode int, and its `UIEvent` callbacks for the actions this port has.
 * The geometry — 24dp outer caps, 6dp inner corners, 3dp gaps, 48dp height — is
 * upstream's, untouched.
 */
@Composable
internal fun ExpressiveConnectedGroup(
    state: M3EState,
    actions: M3EActions,
    onOpenOutput: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val startCap = expressiveRounded(topStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 24.dp)
    val endCap = expressiveRounded(topStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp)
    val middle = expressiveRounded(6.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp),
    ) {
        ExpressiveConnectedSlot(
            shape = startCap,
            onClick = { actions.onShowMoreSheet() },
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        // Output. Upstream opens a sheet of outputs and Cast receivers; this app
        // has no cast state in the port yet, so the slot keeps its place and its
        // tint rather than the row losing a slot and the caps changing shape.
        ExpressiveConnectedSlot(
            shape = middle,
            onClick = onOpenOutput,
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        ExpressiveConnectedSlot(
            shape = middle,
            active = state.controllerState.shuffleEnabled,
            onClick = actions.onToggleShuffle,
        ) {
            Crossfade(targetState = state.controllerState.shuffleEnabled, label = "expressiveShuffle") { isShuffle ->
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = if (isShuffle) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        ExpressiveConnectedSlot(
            shape = middle,
            active = state.controllerState.repeatMode != 0,
            onClick = actions.onCycleRepeat,
        ) {
            Crossfade(targetState = state.controllerState.repeatMode, label = "expressiveRepeat") { mode ->
                when (mode) {
                    1 -> Icon(
                        imageVector = Icons.Rounded.RepeatOne,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = if (mode == 0) {
                            colorScheme.onSurfaceVariant
                        } else {
                            colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        ExpressiveConnectedSlot(
            shape = middle,
            onClick = { actions.onShowQueue() },
        ) {
            Icon(
                imageVector = Icons.Rounded.PlaylistAdd,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        ExpressiveConnectedSlot(
            shape = endCap,
            onClick = { actions.onShowQueue() },
        ) {
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Upstream `ExpressiveConnectedSlot`: a filled surface, tinted primary-container
 * while active, one of a row of equal slots.
 */
@Composable
private fun RowScope.ExpressiveConnectedSlot(
    shape: Shape,
    onClick: (() -> Unit)?,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val container = if (active) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = container,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    } else {
        Surface(
            shape = shape,
            color = container,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/** Upstream `rounded(...)`: a corner-radius helper, kept as one so the port reads the same. */
internal fun expressiveRounded(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
): Shape = RoundedCornerShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart,
)

/** Upstream `rounded(size)`: the same radius on all four corners. */
internal fun expressiveRounded(size: Dp): Shape = RoundedCornerShape(size)
