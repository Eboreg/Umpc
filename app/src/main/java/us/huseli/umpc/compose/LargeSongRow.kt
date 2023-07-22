package us.huseli.umpc.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.mpd.highlight
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun LargeSongRow(
    modifier: Modifier = Modifier,
    albumArtModifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    albumArt: ImageBitmap?,
    isExpanded: Boolean,
    isSelected: Boolean,
    position: Int? = null,
    discNumber: Int? = null,
    artist: String? = null,
    album: String? = null,
    highlight: String? = null,
    removable: Boolean = false,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onGotoAlbumClick: (() -> Unit)? = null,
    onGotoArtistClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val tonalElevation = LocalAbsoluteTonalElevation.current + if (isCurrentSong) 5.dp else 0.dp
    val density = LocalDensity.current
    val swipeEndPx = with(density) { -50.dp.toPx() }
    val swipeAnchors = mapOf(0f to "start", swipeEndPx to "end")
    val swipeableState = rememberSwipeableState(
        initialValue = "start",
        confirmStateChange = {
            if (it == "end") {
                onRemove?.invoke()
                false
            } else true
        }
    )

    var boxModifier = modifier.height(IntrinsicSize.Min)
    if (removable) boxModifier = boxModifier.swipeable(
        state = swipeableState,
        anchors = swipeAnchors,
        orientation = Orientation.Horizontal,
        resistance = ResistanceConfig(0f, 0f, 0f),
    )

    var surfaceModifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    if (isSelected) surfaceModifier = surfaceModifier.border(width = 3.dp, color = MaterialTheme.colorScheme.primary)
    if (removable) surfaceModifier = surfaceModifier.offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }

    CompositionLocalProvider(LocalAbsoluteTonalElevation provides tonalElevation) {
        Box(modifier = boxModifier) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
                content = { Icon(Icons.Sharp.Delete, null) },
            )
            Surface(modifier = surfaceModifier) {
                if (isExpanded) {
                    ExpandedSongRow(
                        // modifier = modifier,
                        song = song,
                        isCurrentSong = isCurrentSong,
                        playerState = playerState,
                        position = position,
                        discNumber = discNumber,
                        showAlbumArt = true,
                        onPlayPauseClick = onPlayPauseClick,
                        onEnqueueClick = onEnqueueClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onGotoArtistClick = onGotoArtistClick,
                        onGotoAlbumClick = onGotoAlbumClick,
                        albumArt = albumArt,
                        highlight = highlight,
                    )
                } else {
                    LargeSongRowContent(
                        // modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        song = song,
                        isCurrentSong = isCurrentSong,
                        playerState = playerState,
                        albumArt = albumArt,
                        albumArtModifier = albumArtModifier,
                        position = position,
                        discNumber = discNumber,
                        artist = artist,
                        album = album,
                        highlight = highlight,
                        onPlayPauseClick = onPlayPauseClick,
                    )
                }
            }
        }
    }
}

@Composable
fun LargeSongRowContent(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    albumArt: ImageBitmap?,
    albumArtModifier: Modifier = Modifier,
    position: Int? = null,
    discNumber: Int? = null,
    artist: String? = null,
    album: String? = null,
    highlight: String? = null,
    onPlayPauseClick: () -> Unit,
) {
    val titleRow =
        if (discNumber != null && position != null) "${discNumber}-${position}. ${song.title}"
        else if (position != null) "${position}. ${song.title}"
        else song.title

    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            imageBitmap = albumArt,
            altIcon = Icons.Sharp.MusicNote,
            forceSquare = true,
            modifier = albumArtModifier
        )
        Column(modifier = Modifier.padding(start = 8.dp).padding(vertical = 8.dp).weight(1f)) {
            Text(
                text = titleRow.highlight(highlight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    LargeSongRowArtistAlbumInfo(artist, album, highlight)
                }
                if (song.duration != null || song.year != null) {
                    if (isInLandscapeMode() || artist == null || album == null) {
                        Text(
                            text = listOfNotNull(
                                song.duration?.formatDuration(),
                                song.year?.toString()
                            ).joinToString(" • "),
                            fontSize = 12.sp,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            song.duration?.formatDuration()?.let {
                                Text(text = it, fontSize = 12.sp, textAlign = TextAlign.End)
                            }
                            song.year?.let {
                                Text(text = it.toString(), fontSize = 12.sp, textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.width(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPauseClick) {
                if (isCurrentSong && playerState == PlayerState.PLAY)
                    Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
                else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
            }
        }
    }
}

@Composable
fun LargeSongRowArtistAlbumInfo(artist: String?, album: String?, highlight: String?) {
    if (artist != null || album != null) {
        val fontSize = 12.sp
        val color = MaterialTheme.colorScheme.onSurfaceVariant

        if (isInLandscapeMode()) {
            Text(
                text = listOfNotNull(artist, album).joinToString(" • ").highlight(highlight),
                fontSize = fontSize,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            if (artist != null) {
                Text(
                    text = artist.highlight(highlight),
                    fontSize = fontSize,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (album != null) {
                Text(
                    text = album.highlight(highlight),
                    fontSize = fontSize,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
