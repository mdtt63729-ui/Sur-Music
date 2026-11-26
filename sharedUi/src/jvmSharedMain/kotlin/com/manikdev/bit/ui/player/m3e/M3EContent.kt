package com.manikdev.bit.ui.player.m3e

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.manikdev.bit.data.model.LikeStatus
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The M3 Expressive now playing content, ported from limusic-android.
 *
 * The layout is upstream's `NowPlayingM3ExpressiveLayout`: the sleeve in a large
 * rounded card over a tonal page, the track and its credits in a row with a
 * filled tonal circle for the like button, the wavy seek bar with its two times,
 * the expressive transport (two secondary-container pills either side of a
 * primary play button whose corner morphs with playback), and the connected
 * six-slot control group underneath.
 *
 * Two things are this app's rather than upstream's, both because the libraries
 * under them differ and neither changes what is on screen:
 *
 *  * [M3EWavySeekBar] draws the wave itself. Upstream uses material3's
 *    `LinearWavyProgressIndicator`, which needs material3 1.4; this app is on
 *    1.3.1, and moving the whole app to 1.4 to get one slider is not a trade
 *    worth making. Same amplitude behaviour, same thumb morph.
 *  * The artwork pager is a single sleeve: this player is drawn inside a sheet
 *    that owns the swipe between tracks.
 */
@Composable
fun M3EPlayer(
    state: M3EState,
    actions: M3EActions,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val song = state.song
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .padding(horizontal = 20.dp),
    ) {
        // Header: dismiss, caption, and a slot on the right so the caption centres.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(onClick = actions.onDismiss),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Now playing",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(40.dp))
        }

        Spacer(Modifier.weight(0.04f))

        // The sleeve: one large rounded card, the page's tonal surface behind it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(30.dp))
                .background(colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = state.screenData.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
        }

        Spacer(Modifier.weight(0.05f))

        M3ETrackInfoRow(state = state, actions = actions)

        Spacer(Modifier.height(10.dp))

        M3EWavySeekBar(
            progressFraction = state.sliderValue,
            isPlaying = state.controllerState.isPlaying,
            activeColor = colorScheme.primary,
            trackColor = colorScheme.secondaryContainer,
            thumbColor = colorScheme.primary,
            onSliderChange = { actions.onSeekFraction(it / 100f) },
            onSliderChangeFinished = {},
        )

        // Time row, 8dp closer to the wave than the bar's touch area, as upstream.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-8).dp),
        ) {
            Text(
                text = elapsedLabel(state.timelineState.positionMs),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Left,
            )
            Text(
                text = lengthLabel(state.timelineState.durationMs),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Right,
            )
        }

        Spacer(Modifier.height(8.dp))

        M3ETransportRow(
            controllerState = state.controllerState,
            loading = state.controllerState.isLoading,
            actions = actions,
        )

        Spacer(Modifier.height(14.dp))

        ExpressiveConnectedGroup(state = state, actions = actions, onOpenOutput = {})

        Spacer(Modifier.weight(0.03f))
    }
}

/**
 * Upstream `ExpressiveTrackInfoRow`: the title over its credits, and the like
 * button as a 48dp filled tonal circle at the end.
 */
@Composable
private fun M3ETrackInfoRow(state: M3EState, actions: M3EActions) {
    val colorScheme = MaterialTheme.colorScheme
    val liked = state.controllerState.likeStatus == LikeStatus.LIKE
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.screenData.title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = state.screenData.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(8.dp))
        Surface(
            onClick = actions.onToggleLike,
            shape = CircleShape,
            color = if (liked) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (liked) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// Upstream's transport weights and growth.
private const val SIDE_WEIGHT = 0.55f
private const val PLAY_WEIGHT = 1.2f
private const val PRESS_GROWTH = 1.15f
private const val TOGGLE_WEIGHT = 0.4f
private val TOGGLE_SEPARATION = 8.dp

/**
 * Upstream `ExpressiveTransportRow`: previous and next as secondary-container
 * pills, play in the primary colour with its corner morphing 22dp while playing
 * and 34dp while paused, each button growing 15% under a press while the row's
 * weights redistribute.
 */
@Composable
private fun M3ETransportRow(
    controllerState: M3EControlState,
    loading: Boolean,
    actions: M3EActions,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val prevPressed by prevInteraction.collectIsPressedAsState()
    val playPressed by playInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()

    val pressSpec = tween<Float>(durationMillis = 220)
    val prevWeight by animateFloatAsState(
        targetValue = if (prevPressed) SIDE_WEIGHT * PRESS_GROWTH else SIDE_WEIGHT,
        animationSpec = pressSpec,
        label = "prevWeight",
    )
    val playWeight by animateFloatAsState(
        targetValue = if (playPressed) PLAY_WEIGHT * PRESS_GROWTH else PLAY_WEIGHT,
        animationSpec = pressSpec,
        label = "playWeight",
    )
    val nextWeight by animateFloatAsState(
        targetValue = if (nextPressed) SIDE_WEIGHT * PRESS_GROWTH else SIDE_WEIGHT,
        animationSpec = pressSpec,
        label = "nextWeight",
    )
    val playCorner by animateDpAsState(
        targetValue = if (controllerState.isPlaying) 22.dp else 34.dp,
        animationSpec = tween(durationMillis = 260),
        label = "playCorner",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
    ) {
        Surface(
            onClick = { if (controllerState.hasPrevious) actions.onPrevious() },
            shape = RoundedCornerShape(34.dp),
            color = colorScheme.secondaryContainer,
            interactionSource = prevInteraction,
            modifier = Modifier
                .weight(prevWeight)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer.copy(
                        alpha = if (controllerState.hasPrevious) 1f else 0.4f,
                    ),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Surface(
            onClick = { if (!loading) actions.onPlayPause() },
            shape = RoundedCornerShape(playCorner),
            color = colorScheme.primary,
            interactionSource = playInteraction,
            modifier = Modifier
                .weight(playWeight)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Crossfade(
                    targetState = loading,
                    animationSpec = tween(180),
                    label = "playLoading",
                    modifier = Modifier.fillMaxSize(),
                ) { isLoading ->
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    } else {
                        Crossfade(
                            targetState = controllerState.isPlaying,
                            animationSpec = tween(180),
                            label = "playPauseIcon",
                            modifier = Modifier.fillMaxSize(),
                        ) { isPlaying ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Surface(
            onClick = { if (controllerState.hasNext) actions.onNext() },
            shape = RoundedCornerShape(34.dp),
            color = colorScheme.secondaryContainer,
            interactionSource = nextInteraction,
            modifier = Modifier
                .weight(nextWeight)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer.copy(
                        alpha = if (controllerState.hasNext) 1f else 0.4f,
                    ),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/**
 * Upstream `WavySeekBar`, drawn rather than taken from material3.
 *
 * The wave ripples while the track plays and flattens to a straight line when it
 * is paused or while a finger is on it, exactly as upstream's amplitude does; the
 * thumb morphs from a 14dp circle into a 6x22dp rounded bar under the finger. The
 * pointer layer is a 40dp tall target over a thinner line, and it maps taps and
 * drags onto a 0..100 scale, which is what [onSliderChange] is fed.
 */
@Composable
private fun M3EWavySeekBar(
    progressFraction: Float,
    isPlaying: Boolean,
    activeColor: Color,
    trackColor: Color,
    thumbColor: Color,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isInteracting by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val displayedFraction = (if (isInteracting) dragFraction else progressFraction).coerceIn(0f, 1f)

    val thumbMorph by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250),
        label = "wavySeekBarThumbMorph",
    )
    // A slow drift along the wave, so the line breathes while it plays.
    val wavePhase = rememberInfiniteTransition(label = "m3eWave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
        ),
        label = "m3eWavePhase",
    )
    val amplitude = if (isPlaying && !isInteracting) 1f else 0f

    fun fractionAt(x: Float): Float = if (widthPx <= 0) 0f else (x / widthPx).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = fractionAt(offset.x)
                    dragFraction = fraction
                    onSliderChange(fraction * 100f)
                    onSliderChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isInteracting = true
                        val fraction = fractionAt(offset.x)
                        dragFraction = fraction
                        onSliderChange(fraction * 100f)
                    },
                    onDragEnd = {
                        isInteracting = false
                        onSliderChangeFinished()
                    },
                    onDragCancel = {
                        isInteracting = false
                        onSliderChangeFinished()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = fractionAt(change.position.x)
                        dragFraction = fraction
                        onSliderChange(fraction * 100f)
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxWidth().height(40.dp)) {
            val centerY = size.height / 2f
            val stroke = 6.dp.toPx()
            val waveLength = 42.dp.toPx()
            val waveHeight = 5.dp.toPx() * amplitude
            val phase = wavePhase.value
            val progressX = size.width * displayedFraction

            // The track under the whole bar.
            drawLine(
                color = trackColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            if (progressX <= 0f) return@Canvas

            // The played part, as a wave whose ripple fades in past the first
            // few pixels so the start of the track does not wiggle on its own.
            val path = Path()
            path.moveTo(0f, centerY)
            var x = 0f
            while (x <= progressX) {
                val ripple = (x / waveLength) * 2f * PI.toFloat() + phase
                path.lineTo(x, centerY + sin(ripple) * waveHeight)
                x += 3f
            }
            path.lineTo(progressX, centerY)
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        // The thumb: a circle that stretches into a bar under the finger.
        val thumbWidth = (14f + (6f - 14f) * thumbMorph).dp
        val thumbHeight = (14f + (22f - 14f) * thumbMorph).dp
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((widthPx - thumbWidth.toPx()) * displayedFraction).roundToInt(),
                        y = ((40.dp - thumbHeight) / 2).roundToPx(),
                    )
                }
                .size(width = thumbWidth, height = thumbHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(thumbColor),
        )
    }
}

/** "3:07" — the elapsed side of the time row. */
private fun elapsedLabel(positionMs: Long): String = formatMs(positionMs)

/** "4:12" — the length side of the time row. */
private fun lengthLabel(durationMs: Long): String = if (durationMs > 0) formatMs(durationMs) else "--:--"

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
