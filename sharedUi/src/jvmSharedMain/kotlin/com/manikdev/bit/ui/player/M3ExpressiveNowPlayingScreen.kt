package com.manikdev.bit.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.manikdev.bit.data.model.LikeStatus
import com.manikdev.bit.data.model.Song
import com.manikdev.bit.playback.PlaybackPosition

/**
 * The Material 3 Expressive player, after the layout in limusic-android's
 * [NowPlayingContentM3Expressive]: a tonal page rather than artwork bleeding to
 * the edges, the sleeve in a large rounded card, the track and its credits in a
 * row with tonal circle buttons, then the scrubber and a transport whose
 * shuffle and repeat are filled pills.
 *
 * Like [ClassicNowPlayingScreen] this is a content swap inside the sheet the
 * Apple Music player is mounted in, so the motion between the mini player and
 * the full one is the same animation — open, close and the shared artwork all
 * come from the sheet. It also reuses [PlayerScrubber] and [TransportRow], so
 * seeking and the play button animate exactly as they do there.
 *
 * The tones are fixed rather than taken from the theme: the transport's glyphs
 * are white, and a light theme's surface would leave them invisible. Material 3
 * Expressive is a dark, tonal treatment, and this draws it as one.
 */
@Composable
fun M3ExpressiveNowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    likeStatus: LikeStatus,
    onToggleLike: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remoteArt = rememberRemoteArtworkUrl(song)
    val scrub = rememberPlayerScrub(song.videoId, position, durationMs)
    val artistLine = song.artists.joinToString(" • ") { it.name }
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(M3E_PAGE),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(M3E_CONTAINER)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = M3E_INK,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = M3E_INK.copy(alpha = 0.6f),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.weight(0.05f))

            // The sleeve, in a card rather than bleeding: expressive keeps its
            // colour on the card and leaves the page a flat tone.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(M3E_CONTAINER),
            ) {
                AsyncImage(
                    model = remoteArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(24.dp)),
                )
            }

            Spacer(Modifier.weight(0.07f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = M3E_INK,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artistLine.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = artistLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = M3E_INK.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))
                // The expressive treatment's filled tonal circle, which is what
                // makes the row read as Material 3 rather than as a bare icon.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (likeStatus == LikeStatus.LIKE) {
                                accent.copy(alpha = 0.28f)
                            } else {
                                M3E_CONTAINER
                            },
                        )
                        .clickable(onClick = onToggleLike),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (likeStatus == LikeStatus.LIKE) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = null,
                        tint = if (likeStatus == LikeStatus.LIKE) accent else M3E_INK.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            PlayerScrubber(
                shown = { scrub.shown(position.positionMs, durationMs) },
                durationMs = durationMs,
                loading = isLoading,
                mixing = false,
                transitionWindow = null,
                onScrub = scrub::drag,
                onScrubFinished = { scrub.release(onSeekFraction) },
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TonalControl(
                    icon = Icons.Rounded.Shuffle,
                    active = shuffleEnabled,
                    accent = accent,
                    onClick = onToggleShuffle,
                )
                Box(Modifier.weight(1f)) {
                    TransportRow(
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        previousEnabled = hasPrevious,
                        nextEnabled = hasNext,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                    )
                }
                TonalControl(
                    icon = if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    active = repeatMode != 0,
                    accent = accent,
                    onClick = onCycleRepeat,
                )
            }
        }
    }
}

/** A filled tonal circle holding one glyph — the expressive transport's shape. */
@Composable
private fun TonalControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.28f) else M3E_CONTAINER)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) accent else M3E_INK.copy(alpha = 0.75f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Material 3's baseline dark surface, and the container one step above it. */
private val M3E_PAGE = Color(0xFF141218)

private val M3E_CONTAINER = Color(0xFF211F26)

private val M3E_INK = Color(0xFFE6E0E9)
