package com.manikdev.bit.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manikdev.bit.R
import com.manikdev.bit.sharedui.resources.Res
import com.manikdev.bit.sharedui.resources.spotify_logo
import com.manikdev.bit.data.model.ShelfItem
import com.manikdev.bit.data.settings.AppSettings
import com.manikdev.bit.download.Downloads
import com.manikdev.bit.download.SavedCollection
import com.manikdev.bit.ui.components.PAGE_GUTTER
import com.manikdev.bit.ui.components.ReplayCardRowSkeleton
import com.manikdev.bit.ui.replay.ReplayCardRow
import com.manikdev.bit.ui.replay.ReplayHeroCard
import com.manikdev.bit.ui.replay.ReplayStoryPage

// The phone's halves of the shared Library page: its list of folders, what
// sits on its "On device" shelf, and the way in to Replay at its head.

/**
 * The phone's folders, drawn as a list under the Replay cards: Downloads and
 * Local Music, the song cache when Settings has it shown, and the two remote
 * libraries it can reach.
 */
@Composable
fun libraryLinks(): List<LibraryLink> {
    val webdavConfigured by AppSettings.webdavUrl.collectAsStateWithLifecycle()
    val smbHost by AppSettings.smbHost.collectAsStateWithLifecycle()
    val smbShare by AppSettings.smbShare.collectAsStateWithLifecycle()
    val showCacheFolder by AppSettings.showCacheFolder.collectAsStateWithLifecycle()
    val spotifyConnected by AppSettings.spotifySpdcToken.collectAsStateWithLifecycle()
    fun link(title: String, subtitle: String, browseId: String, icon: ImageVector = deviceFolderIcon(browseId) ?: Icons.Rounded.Cloud) = LibraryLink(
        item = ShelfItem(
            title = title,
            subtitle = subtitle,
            thumbnailUrl = null,
            videoId = null,
            browseId = browseId,
        ),
        icon = icon,
    )
    return listOfNotNull(
        // Explore and Radio used to be tabs of the bottom bar. They are pages
        // of the Library now, so they open from its folder list like any other
        // place, and the bar keeps Home, Library and Search.
        link(
            stringResource(R.string.explore),
            stringResource(R.string.explore_subtitle),
            EXPLORE_LINK_ID,
            Icons.Rounded.Explore,
        ),
        link(
            stringResource(R.string.radio),
            stringResource(R.string.radio_subtitle),
            RADIO_LINK_ID,
            Icons.Rounded.Radio,
        ),
        link(
            stringResource(R.string.downloads),
            stringResource(R.string.downloaded_songs),
            "local:downloads",
        ),
        link(
            stringResource(R.string.local_music),
            stringResource(R.string.audio_files_on_device),
            "local:all",
        ),
        // Opt-in from Settings → Storage: what the song cache is holding from
        // YouTube and JioSaavn. See [com.manikdev.bit.playback.AudioCache.cachedSongs].
        link(
            stringResource(R.string.cached_songs),
            stringResource(R.string.cached_songs_subtitle),
            CACHE_FOLDER_BROWSE_ID,
        ).takeIf { showCacheFolder },
        // Only once set up in Sources; unconfigured, they'd just be dead ends.
        link(
            stringResource(R.string.webdav),
            stringResource(R.string.webdav_subtitle),
            com.manikdev.bit.data.webdav.WebDavConfig.BROWSE_ID,
        ).takeIf { webdavConfigured.isNotBlank() },
        link(
            stringResource(R.string.smb),
            stringResource(R.string.smb_subtitle),
            com.manikdev.bit.data.smb.SmbConfig.BROWSE_ID,
        ).takeIf { smbHost.isNotBlank() && smbShare.isNotBlank() },
        // Only while signed in to Spotify in Settings → Accounts; it opens that
        // account's playlists rather than a browse page.
        link(
            stringResource(R.string.spotify),
            stringResource(R.string.spotify_library_subtitle),
            SPOTIFY_BROWSE_ID,
        ).takeIf { spotifyConnected.isNotBlank() }?.copy(logo = Res.drawable.spotify_logo),
    )
}

/**
 * The phone's "On device" shelf: the playlists and albums downloaded whole —
 * the same promise the folders above it make, here, now, without a network.
 * Nothing is truncated: the shelf is a row that scrolls, so "all of them"
 * costs nothing.
 */
@Composable
fun libraryDeviceItems(downloadedReleases: List<SavedCollection>): List<ShelfItem> {
    val downloadedPlaylist = stringResource(R.string.downloaded_playlist)
    return downloadedReleases.map { release ->
        ShelfItem(
            title = release.title,
            // The credit the release was downloaded with, because this is also
            // what the page it opens bills itself by — see `headerLines`, which
            // reads the kind and the owner back out of it. Saying "Downloaded
            // playlist" here instead would make that header read "Downloaded
            // playlist" over "PLAYLIST • 12 SONGS", and the shelf this card is on
            // already says where it lives.
            subtitle = release.subtitle.ifBlank { if (release.playlist) downloadedPlaylist else "" },
            thumbnailUrl = release.thumbnailUrl,
            videoId = null,
            browseId = Downloads.pageIdFor(release.id),
        )
    }
}

/**
 * The glyph a device folder is drawn with — its row on the Library page, and
 * the placeholder cover its own page opens on. Null for anything that isn't
 * one of the folders.
 */
fun deviceFolderIcon(browseId: String): ImageVector? = when (browseId) {
    "local:downloads" -> Icons.Rounded.Download
    "local:all" -> Icons.Rounded.Folder
    CACHE_FOLDER_BROWSE_ID -> Icons.Rounded.Storage
    com.manikdev.bit.data.webdav.WebDavConfig.BROWSE_ID -> Icons.Rounded.Cloud
    com.manikdev.bit.data.smb.SmbConfig.BROWSE_ID -> Icons.Rounded.Lan
    else -> null
}

/** The Library row that opens the connected Spotify account; handled by the app, not a browse page. */
/** The Library row that opens Explore. Not a browse page, so it carries its own id. */
const val EXPLORE_LINK_ID = "app:explore"

/** The Library row that opens Radio. */
const val RADIO_LINK_ID = "app:radio"

const val SPOTIFY_BROWSE_ID = "app:spotify"

/** The Cached songs folder's page id — one of the `local:` device folders. */
const val CACHE_FOLDER_BROWSE_ID = "local:cache"

/**
 * The phone's way in to Replay: its row of headline cards, placeholders for
 * them while the history is read, and nothing at all while there is not yet
 * enough listening to deal them — Settings still opens Replay either way.
 */
@Composable
fun LibraryReplayEntry(
    cards: List<ReplayHeroCard>,
    loading: Boolean,
    holder: String,
    memberSince: String?,
    onOpenReplay: (ReplayStoryPage) -> Unit,
) {
    if (loading) {
        ReplayCardRowSkeleton()
    } else if (cards.isNotEmpty()) {
        ReplayCardRow(
            cards = cards,
            holder = holder,
            memberSince = memberSince,
            onCardClick = onOpenReplay,
            modifier = Modifier.padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        )
    }
}
