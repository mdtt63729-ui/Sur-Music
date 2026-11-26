package com.manikdev.bit.ui.player.m3e

import androidx.compose.runtime.Stable
import com.manikdev.bit.data.model.LikeStatus
import com.manikdev.bit.data.model.Song
import com.manikdev.bit.playback.PlaybackPosition

/**
 * The state the M3 Expressive content layer reads.
 *
 * A line-for-line port of limusic-android's `NowPlayingContentState`
 * (`docs/player-port/limusic/NowPlayingContentState.kt.txt`), with the fields
 * this app cannot supply dropped rather than faked: there is no artwork pager,
 * no canvas, no cast state and no lyrics clock on this side of the port yet, and
 * a null there is honest where a fake would put a wrong value on screen.
 *
 * Every field that survives keeps the upstream name and meaning, so the layout
 * file can be read against the original side by side.
 */
@Stable
class M3EState(
    /** Upstream `screenData`. */
    val screenData: M3EScreenData,
    /** Upstream `controllerState`. */
    val controllerState: M3EControlState,
    /** Upstream `timelineState`: where the playhead is, and how long the track is. */
    val timelineState: M3ETimeline,
    /** Upstream `shouldShowVideo`; this app plays audio here, so it is false. */
    val shouldShowVideo: Boolean = false,
    /** Upstream `artworkQueue`: the queue, for the pager this port does not have yet. */
    val artworkQueue: List<Song> = emptyList(),
    val currentOrderIndex: Int = 0,
    /** Upstream `sliderValue`: 0..1 along the track. */
    val sliderValue: Float = 0f,
    /** Upstream `showControlLayout`: the info block fades with this. */
    val showControlLayout: Boolean = true,
    val controlLayoutAlpha: Float = 1f,
    val showHideMiddleLayout: Boolean = true,
    val shouldShowToolbar: Boolean = true,
    val isExpanded: Boolean = true,
    val isShort: Boolean = false,
    val audioQualityLabel: String? = null,
    val videoAspectRatio: Float = 16f / 9,
    val canSwitchVersion: Boolean = false,
) {
    /** The track now playing, as the layout reaches for it. */
    val song: Song get() = screenData.song
}

/** Upstream `NowPlayingScreenData`, cut to what a content layer draws. */
@Stable
data class M3EScreenData(
    val song: Song,
    val title: String,
    val artistName: String,
    val albumName: String?,
    val thumbnailUrl: String?,
    val isExplicit: Boolean = false,
)

/** Upstream `ControlState`, cut to the transport the layout reads. */
@Stable
data class M3EControlState(
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val likeStatus: LikeStatus,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    /** The stream is resolving, so the play glyph would be a lie. */
    val isBuffering: Boolean = isLoading,
)

/** Upstream `TimeLine`. */
@Stable
data class M3ETimeline(
    val positionMs: Long,
    val durationMs: Long,
)

/**
 * Everything the content layer can do. Upstream `NowPlayingContentActions`, with
 * the callbacks this app has no target for left out — the layout is ported
 * against this, so a missing action is a missing button rather than a stub that
 * silently does nothing.
 */
@Stable
class M3EActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeekFraction: (Float) -> Unit,
    val onToggleShuffle: () -> Unit,
    val onCycleRepeat: () -> Unit,
    val onToggleLike: () -> Unit,
    val onDismiss: () -> Unit,
    val onNavigateToArtist: () -> Unit = {},
    val onNavigateToAlbum: () -> Unit = {},
    val onShowQueue: () -> Unit = {},
    val onShowMoreSheet: () -> Unit = {},
)

/** Builds the state a screen passes down from this app's player. */
fun m3eStateOf(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    likeStatus: LikeStatus,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
): M3EState {
    val duration = durationMs.coerceAtLeast(0L)
    val fraction = if (duration > 0L) {
        (position.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }
    return M3EState(
        screenData = M3EScreenData(
            song = song,
            title = song.title,
            artistName = song.artists.joinToString(" • ") { it.name },
            albumName = null,
            thumbnailUrl = song.thumbnailUrl,
            isExplicit = false,
        ),
        controllerState = M3EControlState(
            isPlaying = isPlaying,
            isLoading = isLoading,
            likeStatus = likeStatus,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            hasPrevious = hasPrevious,
            hasNext = hasNext,
        ),
        timelineState = M3ETimeline(positionMs = position.positionMs, durationMs = duration),
        sliderValue = fraction,
    )
}
