package us.huseli.umpc.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDSong
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
        velocityThreshold = Dp.Infinity,
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
                        onRemoveClick = onRemove,
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
