package com.manikdev.bit.ui.screens

import com.manikdev.bit.sharedui.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import com.manikdev.bit.ui.components.ShelfRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manikdev.bit.data.YtMusicRepository
import com.manikdev.bit.data.model.HomeShelf
import com.manikdev.bit.data.model.LibraryPage
import com.manikdev.bit.data.model.ShelfItem
import com.manikdev.bit.data.model.UiState
import com.manikdev.bit.data.settings.LibrarySort
import com.manikdev.bit.ui.AppUi
import com.manikdev.bit.ui.icons.BitChordIcons
import com.manikdev.bit.ui.components.LIBRARY_GRID_SPACING
import com.manikdev.bit.ui.components.MessageState
import com.manikdev.bit.ui.components.PAGE_GUTTER
import com.manikdev.bit.ui.components.PullToRefresh
import com.manikdev.bit.ui.components.SHELF_CARD_WIDTH
import com.manikdev.bit.ui.components.libraryGrid
import com.manikdev.bit.ui.components.librarySkeleton

/**
 * The signed-in library: the saved collections, as shelves of cards.
 *
 * Deliberately only the collections. This page used to end with two runs of
 * track rows — "Liked Music" and "Songs" — which are two overlapping answers
 * to the same question and read as one list that couldn't make up its mind: a
 * track that stopped being liked didn't leave the page, it moved down it, into
 * a section most people had taken for more of the same. Liked Music is a
 * playlist, and it is reached the way every other playlist here is, by opening
 * its card.
 *
 * The liked list is still fetched — it is what the rest of the app reads a
 * track's rating off (see MainViewModel's `likeStatuses`); it just isn't a
 * second place to browse it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    signedIn: Boolean,
    state: UiState<LibraryPage>,
    listState: LazyListState,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onImportSpotifyPlaylist: (() -> Unit)? = null,
    /**
     * A shelf's "Show all" — every shelf's row here stops at five cards (see
     * [LibraryGridShelf]), so this is the only way to reach whatever didn't
     * fit.
     */
    onShowAll: (HomeShelf) -> Unit,
    /**
     * The way in to Replay at the head of the page — the row of Replay cards,
     * their placeholders while the history is read, or nothing at all.
     */
    replay: @Composable () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    /**
     * The device's folders — downloads, local files, the remote libraries this
     * build supports — drawn as a list under the Replay cards. Built by the
     * app, since which of those exist is the platform's business.
     */
    links: List<LibraryLink>,
    /**
     * The "On device" shelf under that list: the releases kept on this device
     * whole. Left off the page entirely while there are none.
     */
    deviceItems: List<ShelfItem>,
    /** The big "Library" heading; the desktop's pages carry none. */
    showTitle: Boolean = true,
) {
    val pinnedPlaylists by AppUi.host.pinnedPlaylists.collectAsStateWithLifecycle()
    val onDevice = stringResource(Res.string.on_device)
    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (showTitle) {
                item {
                    Text(
                        text = stringResource(Res.string.library),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                    )
                }
            }
            item(key = "replay") { replay() }
            if (links.isNotEmpty()) {
                item(key = "links") { LibraryLinkList(links = links, onClick = onShelfItemClick) }
            }
            if (deviceItems.isNotEmpty()) {
                item(key = "shelf:$onDevice") {
                    val onDeviceShelf = HomeShelf(title = onDevice, items = deviceItems)
                    LibraryGridShelf(
                        shelf = onDeviceShelf,
                        onItemClick = onShelfItemClick,
                        onItemLongPress = onShelfItemLongPress,
                        onShowAll = { onShowAll(onDeviceShelf) },
                    )
                }
            }
            if (!signedIn) {
                item(key = "shelf:$PLAYLISTS") {
                    val emptyPlaylists = HomeShelf(PLAYLISTS, emptyList())
                    PlaylistShelf(
                        shelf = emptyPlaylists,
                        onItemClick = onShelfItemClick,
                        onItemLongPress = onShelfItemLongPress,
                        onNewPlaylist = onNewPlaylist,
                        onImportSpotifyPlaylist = onImportSpotifyPlaylist,
                        onShowAll = { onShowAll(emptyPlaylists) },
                    )
                }
                item {
                    MessageState(
                        message = stringResource(Res.string.library_sign_in_description),
                        actionLabel = stringResource(Res.string.sign_in),
                        onAction = onSignIn,
                    )
                }
                return@LazyColumn
            }
            when (state) {
                is UiState.Loading -> librarySkeleton()
                is UiState.Error -> item {
                    MessageState(state.message, actionLabel = stringResource(Res.string.retry), onAction = onRetry)
                }
                is UiState.Success -> {
                    // A fresh account has no Playlists shelf at all, and that
                    // is exactly the account most in need of the button that
                    // makes one — so the row is drawn either way, empty but
                    // for the tile that creates the first playlist.
                    val shelves = state.data.shelves
                    if (shelves.none { it.title == PLAYLISTS }) {
                        item(key = "shelf:$PLAYLISTS") {
                            val emptyPlaylists = HomeShelf(PLAYLISTS, emptyList())
                            PlaylistShelf(
                                shelf = emptyPlaylists,
                                onItemClick = onShelfItemClick,
                                onItemLongPress = onShelfItemLongPress,
                                onNewPlaylist = onNewPlaylist,
                                onImportSpotifyPlaylist = onImportSpotifyPlaylist,
                                onShowAll = { onShowAll(emptyPlaylists) },
                            )
                        }
                    }
                    shelves.forEach { shelf ->
                        item(key = "shelf:${shelf.title}") {
                            if (shelf.title == PLAYLISTS) {
                                val pinnedFirst = shelf.pinnedFirst(pinnedPlaylists)
                                PlaylistShelf(
                                    shelf = pinnedFirst,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onNewPlaylist = onNewPlaylist,
                                    onImportSpotifyPlaylist = onImportSpotifyPlaylist,
                                    onShowAll = { onShowAll(pinnedFirst) },
                                    pinnedPlaylists = pinnedPlaylists,
                                )
                            } else {
                                LibraryGridShelf(
                                    shelf = shelf,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onShowAll = { onShowAll(shelf) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One of the Library's folder rows: the page [item] opens, behind [icon]. */
/** [logo], when set, is drawn in place of [icon] — a service's own mark. */
data class LibraryLink(val item: ShelfItem, val icon: ImageVector, val logo: DrawableResource? = null)

/**
 * The folders — downloads, local files, the remote libraries, and the pages
 * that live under Library — as a two-column grid of cards.
 *
 * Each card is a tile of its own: a rounded square holding the glyph, centred
 * and drawn large, with the name under it. That is the shape the Library's own
 * artwork cards take, so the folders read as part of the same page rather than
 * as a settings list that happens to be here.
 */
@Composable
private fun LibraryLinkList(links: List<LibraryLink>, onClick: (ShelfItem) -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 22.dp)
            .padding(horizontal = PAGE_GUTTER),
        verticalArrangement = Arrangement.spacedBy(LINK_CARD_GAP),
    ) {
        links.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(LINK_CARD_GAP)) {
                pair.forEach { link ->
                    LibraryLinkCard(
                        link = link,
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                // An odd one out keeps the column width it would have had, so
                // the last card does not stretch across the page.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * One folder card: the glyph in its own rounded square at the top, the name
 * under it. Material 3 throughout — the surface, the tile and the type all come
 * from the theme, so a light theme inverts the whole card rather than leaving a
 * dark rectangle on it.
 */
@Composable
private fun LibraryLinkCard(
    link: LibraryLink,
    onClick: (ShelfItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LINK_CARD_CORNER)
    Column(
        modifier = modifier
            .height(LINK_CARD_HEIGHT)
            .clip(shape)
            // Plain Material 3, not the liquid glass modifier: these cards live
            // in a LazyColumn, and a glass surface there samples a backdrop that
            // is scrolling out from under it.
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .border(
                LINK_CARD_BORDER,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape,
            )
            .clickable { onClick(link.item) }
            .padding(LINK_CARD_PADDING),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(LINK_TILE_SIZE)
                .clip(RoundedCornerShape(LINK_TILE_CORNER))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (link.logo != null) {
                // A mark, not a glyph: white on dark, black on light, with its
                // cut-outs left clear.
                Icon(
                    painter = painterResource(link.logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(LINK_GLYPH_SIZE),
                )
            } else {
                Icon(
                    imageVector = link.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(LINK_GLYPH_SIZE),
                )
            }
        }
        Text(
            text = link.item.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val LINK_CARD_CORNER = 26.dp
private val LINK_CARD_HEIGHT = 132.dp
private val LINK_CARD_PADDING = 16.dp
private val LINK_CARD_GAP = 12.dp
private val LINK_CARD_BORDER = 1.dp
/** The glyph's own square, and the glyph inside it. */
private val LINK_TILE_SIZE = 56.dp
private val LINK_TILE_CORNER = 18.dp
private val LINK_GLYPH_SIZE = 30.dp

/**
 * The one shelf on this page that can be written to: it leads with the tile
 * that creates a playlist, and holding a card gets rename and delete on top of
 * the queue actions every other shelf's menu offers.
 */
@Composable
private fun PlaylistShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onImportSpotifyPlaylist: (() -> Unit)? = null,
    onShowAll: () -> Unit,
    pinnedPlaylists: List<String> = emptyList(),
) {
    val creationCards: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING)) {
            NewShelfCard(
                icon = BitChordIcons.Plus,
                label = stringResource(Res.string.new_playlist),
                subtitle = stringResource(Res.string.saved_to_youtube_music),
                onClick = onNewPlaylist,
            )
            if (onImportSpotifyPlaylist != null) {
                NewShelfCard(
                    icon = BitChordIcons.Download,
                    label = stringResource(Res.string.import_spotify),
                    subtitle = stringResource(Res.string.import_spotify_subtitle),
                    onClick = onImportSpotifyPlaylist,
                    logo = Res.drawable.spotify_logo,
                )
            }
        }
    }

    val hasPlaylists = shelf.items.isNotEmpty()

    LibraryGridShelf(
        shelf = shelf,
        onItemClick = onItemClick,
        onItemLongPress = onItemLongPress,
        onShowAll = onShowAll,
        pinnedPlaylists = pinnedPlaylists,
        leadingCard = if (!hasPlaylists) creationCards else null,
        trailingCard = if (hasPlaylists) creationCards else null,
    )
}

/** A Library shelf's preview row never swipes past this many cards. */
private const val LIBRARY_ROW_MAX_ITEMS = 5

/**
 * A Library shelf: a sideways-scrolling row of [SHELF_CARD_WIDTH] cards, the
 * same as every other shelf, but stopped at [LIBRARY_ROW_MAX_ITEMS] rather
 * than left to run the shelf's whole length — with a "Show all" beside the
 * title whenever there's more than that, opening the rest as a
 * vertically-scrolling grid instead. See [LibraryGridPage].
 *
 * [leadingCard], if given, occupies the first slot and counts against that
 * cap — see [PlaylistShelf].
 */
@Composable
internal fun LibraryGridShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    onShowAll: () -> Unit,
    leadingCard: (@Composable () -> Unit)? = null,
    trailingCard: (@Composable () -> Unit)? = null,
    pinnedPlaylists: List<String> = emptyList(),
) {
    val extraCount = (if (leadingCard != null) 1 else 0) + (if (trailingCard != null) 1 else 0)
    val visibleItems = shelf.items.take((LIBRARY_ROW_MAX_ITEMS - extraCount).coerceAtLeast(0))
    val cardKeys = remember(visibleItems) { shelfCardKeys(visibleItems) }
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle,
            onShowAll = onShowAll.takeIf { shelf.items.size + extraCount > LIBRARY_ROW_MAX_ITEMS },
        )
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
            flingBehavior = ScrollableDefaults.flingBehavior(),
        ) {
            leadingCard?.let { card -> item(key = "leading") { card() } }
            items(visibleItems.size, key = { cardKeys[it] }) { index ->
                val item = visibleItems[index]
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    isPinned = item.browseId != null && item.browseId in pinnedPlaylists,
                )
            }
            trailingCard?.let { card -> item(key = "trailing") { card() } }
        }
    }
}

/**
 * One key per card in a shelf row, unique however the cards repeat. A lazy row
 * throws on a key it has seen twice, and two cards can share a browse id (a
 * shelf that lists one twice) or, with none, a title. Prefixed so no card's
 * key can be the row's "leading" or "trailing" card's.
 */
internal fun shelfCardKeys(items: List<ShelfItem>): List<String> {
    val seen = HashMap<String, Int>()
    return items.map { item ->
        val base = "card:" + (item.browseId ?: item.videoId ?: item.title)
        val count = (seen[base] ?: 0) + 1
        seen[base] = count
        if (count == 1) base else "$base#$count"
    }
}

/**
 * Everything a Library shelf's "Show all" opens onto — the same cards, at the
 * same [libraryGrid] width, run down the screen instead of stopping at one row.
 */
@Composable
fun LibraryGridPage(
    shelf: HomeShelf,
    gridState: LazyGridState,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNewPlaylist: (() -> Unit)? = null,
) {
    // Re-read live rather than trusting [shelf] to already be sorted: this page
    // is opened from a snapshot (see `AppRoute.LibraryShowAll`), and a
    // pin toggled from this page's own long-press menu must move the card
    // immediately rather than waiting for the row underneath to be revisited.
    val pinnedPlaylists by AppUi.host.pinnedPlaylists.collectAsStateWithLifecycle()
    val librarySort by AppUi.host.librarySort.collectAsStateWithLifecycle()
    // Pinning wins over the default order, but an explicit sort is a stronger,
    // more deliberate signal than a pin and is left to reorder the whole grid,
    // pinned cards included.
    val sortedShelf = shelf.pinnedFirst(pinnedPlaylists).sortedForLibrary(librarySort)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val grid = libraryGrid(maxWidth - PAGE_GUTTER * 2)
        LazyVerticalGrid(
            columns = GridCells.Fixed(grid.columns),
            state = gridState,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
        ) {
            if (onNewPlaylist != null) {
                item(key = "leading") {
                    NewShelfCard(
                        icon = BitChordIcons.Plus,
                        label = stringResource(Res.string.new_playlist),
                        subtitle = stringResource(Res.string.saved_to_youtube_music),
                        onClick = onNewPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            items(sortedShelf.items, key = { it.browseId ?: it.title }) { item ->
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    modifier = Modifier.fillMaxWidth(),
                    isPinned = item.browseId != null && item.browseId in pinnedPlaylists,
                )
            }
        }
    }
}

/**
 * Moves whichever of this shelf's cards are in [pinned] to the front, in the
 * order they were pinned, leaving everything else in its existing order behind
 * them.
 *
 * A no-op on any shelf that isn't Playlists: [pinned] only ever holds playlist
 * browse ids, so an album or artist shelf never has a card that matches.
 */
private fun HomeShelf.pinnedFirst(pinned: List<String>): HomeShelf {
    if (pinned.isEmpty()) return this
    val byId = items.filter { it.browseId != null }.associateBy { it.browseId }
    val pinnedItems = pinned.mapNotNull { byId[it] }
    if (pinnedItems.isEmpty()) return this
    val pinnedSet = pinnedItems.toSet()
    return copy(items = pinnedItems + items.filter { it !in pinnedSet })
}

/**
 * A card's title is all a Library shelf carries, so [LibrarySort.DEFAULT] is
 * the only option that isn't alphabetical — everything else sorts on it.
 */
private fun HomeShelf.sortedForLibrary(sort: LibrarySort): HomeShelf = when (sort) {
    LibrarySort.DEFAULT -> this
    LibrarySort.TITLE_ASC -> copy(items = items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }))
    LibrarySort.TITLE_DESC -> copy(
        items = items.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }),
    )
}

/** The library feed whose cards are the account's own — see [PlaylistShelf]. */
private const val PLAYLISTS = YtMusicRepository.PLAYLISTS_SHELF
