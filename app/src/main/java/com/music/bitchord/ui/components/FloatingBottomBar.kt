package com.music.bitchord.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs
import kotlin.math.roundToInt

data class BottomTab(
    val label: String,
    val icon: ImageVector,
)

/**
 * The gap between the pill's glass edge and the tabs inside it.
 *
 * Apple's selection sits about 4pt in from its tab bar's edge, measured off
 * the WWDC Liquid Glass frames; it was 8 here, then 6.
 *
 * Shared with [GlassNavBar], which is meant to measure the same as this bar
 * rather than merely near it.
 */
internal val PILL_INSET = 4.dp

/**
 * The tab bar's margin at either side of the screen: Apple's is 19pt, wider
 * than the page's own [BAR_GUTTER].
 */
internal val NAV_BAR_GUTTER = 19.dp

/**
 * How far above [floatingBarInsets] the bar sits: on a gesture inset of 15dp
 * this puts its foot 21dp off the screen's edge, where Apple's is.
 */
internal val NAV_BAR_LIFT = 6.dp

/**
 * How far the floating bars stand off the bottom edge: the system navigation
 * bar's inset, but never less than a gesture bar's.
 *
 * With the gesture hint (or the navigation bar itself) switched off that inset
 * is zero, and the tab bar dropped onto the screen's very edge. It keeps the
 * height it has with the hint showing instead. Three-button navigation is
 * taller than the floor, so it still wins there.
 */
internal val floatingBarInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.union(WindowInsets(bottom = GESTURE_BAR_FLOOR))

/** A gesture bar's inset — 45px at the test phone's 3x density. */
private val GESTURE_BAR_FLOOR = 15.dp

/**
 * Each tab's own vertical padding, and the counterweight to [PILL_INSET].
 *
 * The pill has no height of its own — it is whatever its contents come to — so
 * taking 2dp off the inset above would have shortened the whole bar by 4. The
 * same 2dp is added back here instead, which leaves the bar's outer height
 * exactly where it was and moves the boundary rather than the bar. The two
 * numbers are a pair: change one and the bar's height moves unless the other
 * moves against it.
 */
internal val TAB_VERTICAL_PADDING = 6.5.dp

/**
 * The round Search button beside the capsule, and the gap before it.
 *
 * The Spotify bar splits the row this way: the capsule divides its width
 * between the browse tabs, and Search stands outside it as its own circle, so
 * adding it never squeezes the other four.
 */
internal val SEARCH_BUTTON_SIZE = 56.dp
internal val SEARCH_BUTTON_GAP = 12.dp

/** The gap between a tab's glyph and its label, in both bars. */
internal val TAB_ICON_LABEL_GAP = 1.dp

/**
 * A tab's glyph box. Apple's glyphs stand 21 to 24pt in a bar 61pt tall, a
 * larger share of it than these had at 25dp in 70; at 28 they come out at
 * Apple's sizes, and with the padding above the bar is 62dp.
 */
internal val TAB_ICON_SIZE = 28.dp

/** A tab's label: Apple's 10pt, letter-spaced as SF Pro Text sets it. */
@Composable
internal fun tabLabelStyle(): TextStyle =
    MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp)

/**
 * The spring the selection indicator and the tab glyphs both travel on.
 *
 * Damping 0.72 rather than the 0.5 it was: half-damped overshoots two or three
 * times, and a run of diminishing bounces is what makes a control read as a
 * spring rather than as a material. This settles on the second approach — one
 * soft pass beyond the mark and done — which is the difference between bouncy
 * and alive.
 *
 * Stiffness 320 puts the whole movement at roughly a third of a second, quick
 * enough that the tap and the arrival feel like one event.
 */
internal val GlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

/**
 * How far the indicator elongates along its travel, at full stride.
 *
 * This is the part that reads as liquid rather than as a sliding rectangle. A
 * shape crossing a gap under its own momentum does not stay the shape it was:
 * it draws out along the direction it is going and gathers itself back at the
 * end. Driven off how far there is still to go, so it is widest in the middle
 * of the trip and exactly itself once it arrives — no state to keep, and it
 * falls out of a drag for free, since dragging is nothing but a long way still
 * to go.
 *
 * Sixteen percent is enough to be felt and not enough to be caught at: past
 * about a fifth the pill starts reading as a stretched image of itself.
 */
internal const val STRETCH = 0.16f

/**
 * How much of the stretch is taken back out of the indicator's height.
 *
 * Half, not all. Conserving area exactly is what a drop of water does, and it
 * is too much here — the indicator sits behind a glyph that is not deforming
 * with it, and a full counter-squash reads as the pill being crushed rather
 * than drawn. Half keeps the sense of something with a volume to redistribute
 * while leaving the glyph its ground.
 */
internal const val SQUASH = 0.5f

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    /** Called when the user taps the currently active tab (scroll-to-top). */
    onActiveTabTap: () -> Unit = {},
) {
    val pillShape = RoundedCornerShape(percent = 50)
    val container = MaterialTheme.colorScheme.surface
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    // The glass settle is exactly the motion "reduce animation" promises to
    // drop — snapping both the indicator's travel and the glyph's pop to
    // their target leaves the tap itself instant rather than eased.
    val glassSpec: AnimationSpec<Float> = if (reduceAnimation) snap() else GlassSpring

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    // Search rides in its own circle beside the capsule, the way the Spotify
    // bar splits them: the capsule divides its width between the browse tabs
    // only, so adding Search never squeezes the other four.
    val barTabs = if (tabs.size > 1) tabs.dropLast(1) else tabs
    val searchTab = tabs.lastOrNull()
    val searchIndex = tabs.lastIndex
    // -1 while Search is the selection: nothing inside the capsule is lit then.
    val capsuleSelected = if (selectedIndex <= barTabs.lastIndex) selectedIndex else -1

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val gapPx = with(density) { 6.dp.toPx() }
    val n = barTabs.size

    // With weight(1f) + spacedBy(gap):
    //   tabWidth = (rowWidth - gap*(n-1)) / n
    //   tab i left edge = i * (tabWidth + gap) = i * (rowWidth + gap) / n
    val tabWidthPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width - gapPx * (n - 1)) / n
    } else 0f
    val tabStepPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width + gapPx) / n
    } else 0f

    // The drag below runs in one coroutine for the life of the bar, so it
    // reads everything that can change under it through these. Read plainly,
    // each would stay what it was on the first touch.
    val currentTabStepPx by rememberUpdatedState(tabStepPx)
    val currentLastIndex by rememberUpdatedState(barTabs.lastIndex)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    val pillTargetPx = if (tabStepPx > 0f) {
        capsuleSelected.coerceAtLeast(0) * tabStepPx + dragOffset
    } else 0f

    val animatedPillOffset by animateFloatAsState(
        targetValue = pillTargetPx,
        animationSpec = glassSpec,
        label = "pillOffset",
    )

    // How much of a tab's stride is still ahead of the indicator: 0 at rest,
    // toward 1 in the middle of a move or under a drag that has run away from
    // it. The stretch below is a function of this and nothing else, which is
    // what keeps it honest — the shape can only be deformed while it is
    // actually behind where it is going.
    val lag = if (tabStepPx > 0f) {
        (abs(pillTargetPx - animatedPillOffset) / tabStepPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Held in the capsule's own numbering: while Search is the selection there
    // is no lit tab in the capsule, and a 4 here would tick on the first pixel
    // of every drag.
    var lastHapticTab by remember { mutableIntStateOf(capsuleSelected.coerceAtLeast(0)) }

    LaunchedEffect(selectedIndex) {
        dragOffset = 0f
        lastHapticTab = capsuleSelected.coerceAtLeast(0)
    }

    // One material, two surfaces: the capsule and the Search circle are cut
    // from the same glass, so the row reads as one bar with a gap in it.
    val surface: @Composable () -> Modifier = {
        when {
            reduceDynamicBlur -> Modifier.background(container)
            useGlass -> Modifier.liquidGlass(shape = pillShape)
            else ->
                Modifier.optimizedHazeEffect(
                    state = hazeState,
                    style = HazeMaterials.regular(container),
                )
        }
    }

    Row(
        modifier = modifier
            .windowInsetsPadding(floatingBarInsets)
            .padding(horizontal = NAV_BAR_GUTTER)
            .padding(bottom = NAV_BAR_LIFT)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        // The capsule takes what the Search circle leaves; the pair is centred
        // as one cluster rather than the gap wedging itself into the middle.
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(pillShape)
                .then(surface())
                .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, pillShape)
                .padding(horizontal = PILL_INSET, vertical = PILL_INSET),
        ) {
            // Hidden while Search is selected, so nothing in the capsule sits
            // half-lit behind a tab that is not the one in use.
            if (tabWidthPx > 0f && capsuleSelected >= 0) {
                Box(
                    modifier = Modifier
                        .width(with(density) { tabWidthPx.toDp() })
                        .height(with(density) { rowSize.height.toDp() })
                        .graphicsLayer {
                            translationX = animatedPillOffset
                            // Around its own centre, so the indicator draws out
                            // both ways rather than growing a tail off one edge —
                            // a leading edge that ran ahead of the glyph it is
                            // meant to be behind would read as two things moving,
                            // not one thing stretching.
                            scaleX = 1f + lag * STRETCH
                            scaleY = 1f - lag * STRETCH * SQUASH
                        }
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowSize = it }
                    .pointerInput(Unit) {
                        // Every drag measures from the tab selected when it starts.
                        // A stale index here measured them all from the tab the bar
                        // first opened on: a drag right of it still landed, while
                        // one to the left clamped back onto it and read as no change.
                        var totalDrag = 0f
                        var dragStartIndex = currentSelectedIndex.coerceIn(0, currentLastIndex)
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDrag = 0f
                                dragStartIndex = currentSelectedIndex.coerceIn(0, currentLastIndex)
                            },
                            onDragCancel = { dragOffset = 0f },
                            onDragEnd = {
                                val step = currentTabStepPx
                                if (step > 0f) {
                                    val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                        .coerceIn(0f, currentLastIndex.toFloat())
                                    val newIndex = absoluteIndexFloat.roundToInt()
                                    if (newIndex != currentSelectedIndex) {
                                        currentOnTabSelected(newIndex)
                                    }
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, delta ->
                                val step = currentTabStepPx
                                if (step <= 0f) return@detectHorizontalDragGestures
                                totalDrag += delta
                                val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                    .coerceIn(0f, currentLastIndex.toFloat())
                                dragOffset =
                                    absoluteIndexFloat * step -
                                        currentSelectedIndex.coerceIn(0, currentLastIndex) * step

                                val approxTab = absoluteIndexFloat.roundToInt()
                                if (approxTab != lastHapticTab) {
                                    haptics.play(Haptic.Tick)
                                    lastHapticTab = approxTab
                                }
                            },
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A real glass pill samples whatever artwork is behind it, not the
                // theme's surface color, so a fixed onSurfaceVariant gray can lose
                // contrast against it. Glass mode reads luminance off the surface
                // color instead and picks pure black or white, same as the tint
                // Echo's floating nav bar uses for its own liquid glass.
                val glassTint = glassContentColor()
                val adaptiveTint = if (useGlass) glassTint else null
                barTabs.forEachIndexed { index, tab ->
                    BottomBarItem(
                        tab = tab,
                        selected = index == selectedIndex,
                        glassSpec = glassSpec,
                        selectedTint = adaptiveTint,
                        unselectedTint = adaptiveTint?.copy(alpha = 0.65f),
                        onClick = { onTabSelected(index) },
                        onActiveTabTap = onActiveTabTap,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (searchTab != null) {
            Spacer(Modifier.width(SEARCH_BUTTON_GAP))
            SearchButton(
                tab = searchTab,
                selected = selectedIndex == searchIndex,
                glassTint = if (useGlass) glassContentColor() else null,
                onClick = {
                    if (selectedIndex == searchIndex) {
                        onActiveTabTap()
                    } else {
                        haptics.play(Haptic.Select)
                    }
                    onTabSelected(searchIndex)
                },
                modifier = Modifier.size(SEARCH_BUTTON_SIZE),
                surface = surface,
            )
        }
    }
}

/**
 * The round Search button beside the capsule.
 *
 * Icon only, no label: it is the one tab that is not part of the capsule's
 * row, and a label under a 28dp glyph in a 56dp circle is what the capsule
 * already looks like. Lit while Search is the selected tab, exactly as a
 * capsule tab is.
 */
@Composable
private fun SearchButton(
    tab: BottomTab,
    selected: Boolean,
    glassTint: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The bar's material, applied inside the circle's clip so it fills the round. */
    surface: @Composable () -> Modifier = { Modifier },
) {
    val pillShape = RoundedCornerShape(percent = 50)
    val tint by animateColorAsState(
        targetValue = if (selected) {
            glassTint ?: MaterialTheme.colorScheme.primary
        } else {
            glassTint?.copy(alpha = 0.65f) ?: MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "searchTint",
    )
    Box(
        modifier = modifier
            .clip(pillShape)
            .then(surface())
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, pillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(TAB_ICON_SIZE),
        )
    }
}

@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    glassSpec: AnimationSpec<Float>,
    onClick: () -> Unit,
    onActiveTabTap: () -> Unit,
    modifier: Modifier = Modifier,
    /** Overrides the theme's primary/onSurfaceVariant tint — see the glass branch above. */
    selectedTint: Color? = null,
    unselectedTint: Color? = null,
) {
    // The same spring the indicator rides, so the glyph arriving and the glass
    // arriving are one movement rather than two that nearly agree.
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = glassSpec,
        label = "tabScale",
    )
    val haptics = rememberHaptics()
    val tint by animateColorAsState(
        targetValue = if (selected) {
            selectedTint ?: MaterialTheme.colorScheme.primary
        } else {
            unselectedTint ?: MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "tabTint",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (selected) {
                    onActiveTabTap()
                } else {
                    haptics.play(Haptic.Select)
                }
                onClick()
            }
            .padding(vertical = TAB_VERTICAL_PADDING),
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier
                .size(TAB_ICON_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        Spacer(Modifier.height(TAB_ICON_LABEL_GAP))
        Text(
            text = tab.label,
            style = tabLabelStyle(),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
