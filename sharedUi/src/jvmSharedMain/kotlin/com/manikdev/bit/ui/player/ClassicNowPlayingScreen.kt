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
 * The Classic player: artwork over a dark page, the track and its credits under
 * it, a scrubber, and the transport at the foot — the layout this app's player
 * had before the Apple Music one, drawn from the same pieces the Apple Music
 * player uses.
 *
 * It is a *content* swap, not a second player: the sheet that mounts it, and
 * with it the motion between the mini player and the full one, is the same
 * sheet either style is drawn in (see MainActivity's `nowPlaying` lambda). The
 * open and close therefore animate exactly as they do on the Apple Music
 * player, down to the shared artwork, because it is the same animation running.
 *
 * The scrubber and the transport are [PlayerScrubber] and [TransportRow] rather
 * than new ones: a classic layout that seeked differently, or animated its play
 * button differently, would be a second player to keep correct.
 */
@Composable
fun ClassicNowPlayingScreen(
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CLASSIC_PAGE),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CLASSIC_INK,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onClose),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = CLASSIC_INK.copy(alpha = 0.6f),
                )
                Spacer(Modifier.weight(1f))
                // Balances the chevron so the caption stays centred.
                Spacer(Modifier.size(30.dp))
            }

            Spacer(Modifier.weight(0.06f))

            // The sleeve: one square, centred, the size of the page it can have.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CLASSIC_SLEEVE),
            ) {
                AsyncImage(
                    model = remoteArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.weight(0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = CLASSIC_INK,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artistLine.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = artistLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CLASSIC_INK.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    imageVector = if (likeStatus == LikeStatus.LIKE) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.Rounded.FavoriteBorder
                    },
                    contentDescription = null,
                    tint = if (likeStatus == LikeStatus.LIKE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        CLASSIC_INK.copy(alpha = 0.75f)
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onToggleLike),
                )
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

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = if (shuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        CLASSIC_INK.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onToggleShuffle),
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
                Icon(
                    imageVector = if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = if (repeatMode != 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        CLASSIC_INK.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onCycleRepeat),
                )
            }
        }
    }
}

/** The classic page's own ink: white on a dark page, whatever the theme says. */
private val CLASSIC_INK = Color.White

private val CLASSIC_PAGE = Color(0xFF121212)

private val CLASSIC_SLEEVE = Color(0xFF1E1E1E)
