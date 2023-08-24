package us.huseli.umpc.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Headphones
import androidx.compose.material.icons.sharp.Repeat
import androidx.compose.material.icons.sharp.Shuffle
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.compose.AlbumArt
import us.huseli.umpc.compose.PlayerControls
import us.huseli.umpc.compose.SongProgressSlider
import us.huseli.umpc.compose.VolumeSlider
import us.huseli.umpc.compose.utils.FadingImageBox
import us.huseli.umpc.compose.utils.SmallOutlinedButton
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAudioFormat
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.CurrentSongViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CoverScreen(
    modifier: Modifier = Modifier,
    viewModel: CurrentSongViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val albumArt by viewModel.currentSongAlbumArt.collectAsStateWithLifecycle()
    val audioFormat by viewModel.currentAudioFormat.collectAsStateWithLifecycle()
    val bitrate by viewModel.currentBitrate.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val duration by viewModel.currentSongDuration.collectAsStateWithLifecycle()
    val elapsed by viewModel.currentSongElapsed.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isDynamicPlaylistActive by viewModel.isDynamicPlaylistActive.collectAsStateWithLifecycle(false)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val screenHeightDp = LocalContext.current.resources.configuration.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }
    val anchors = mapOf(0f to "show", screenHeightPx to "hide")
    val landscape = isInLandscapeMode()

    val swipeableState = rememberSwipeableState(
        initialValue = "show",
        confirmStateChange = {
            if (it == "hide") onDismiss()
            true
        }
    )

    Box(
        modifier = modifier
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                orientation = Orientation.Vertical,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                resistance = ResistanceConfig(0f, 0f, 0f),
            )
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, swipeableState.offset.value.roundToInt()) }
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val colorStops =
                if (landscape) arrayOf(
                    0f to MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                    1f to MaterialTheme.colorScheme.background,
                )
                else arrayOf(
                    0.3f to Color.Transparent,
                    1f to MaterialTheme.colorScheme.background,
                )

            FadingImageBox(
                colorStops = colorStops,
                verticalSpacing = 5.dp,
                contentPadding = PaddingValues(0.dp),
                image = { AlbumArt(imageBitmap = albumArt?.fullImage) },
            )

            VolumeSlider(
                volume = volume.toFloat(),
                shape = MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp)),
                padding = PaddingValues(top = 15.dp),
                onVolumeChange = { viewModel.setVolume(it.toInt()) },
                enabled = isConnected,
            )
            CoverScreenSongTechInfo(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                bitrate = bitrate,
                audioFormat = audioFormat,
                isDynamicPlaylistActive = isDynamicPlaylistActive,
            )

            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                CoverScreenSongInfoTexts(currentSong, onGotoAlbumClick, onGotoArtistClick)
                CoverScreenButtons(viewModel)

                PlayerControls(
                    playerState = playerState,
                    stopAfterCurrent = stopAfterCurrent,
                    enabled = isConnected,
                    onPreviousClick = { viewModel.previousOrRestart() },
                    onPlayPauseClick = { viewModel.playOrPause() },
                    onStopClick = { viewModel.stop() },
                    onNextClick = { viewModel.next() },
                    onForwardClick = { viewModel.seekRelative(10.0) },
                    onReverseClick = { viewModel.seekRelative(-10.0) },
                    onStopLongClick = { viewModel.toggleStopAfterCurrent() },
                )
                SongProgressSlider(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    elapsed = elapsed ?: 0.0,
                    duration = duration ?: 0.0,
                    playerState = playerState,
                    shape = MaterialTheme.shapes.medium.copy(
                        bottomEnd = CornerSize(0.dp),
                        bottomStart = CornerSize(0.dp),
                    ),
                    enabled = isConnected,
                    onManualChange = { viewModel.seek(it) },
                )
            }
        }
    }
}

@Composable
fun CoverScreenSongInfoTexts(
    song: MPDSong?,
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
) {
    song?.let {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.clickable { onGotoArtistClick(song.artist) },
                text = song.artist,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.clickable { onGotoAlbumClick(song.album) },
                text = song.album.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverScreenSongTechInfo(
    modifier: Modifier = Modifier,
    bitrate: Int?,
    audioFormat: MPDAudioFormat?,
    isDynamicPlaylistActive: Boolean,
) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = modifier) {
        bitrate?.let { Badge { Text(stringResource(R.string.x_kbps, it)) } }
        if (isDynamicPlaylistActive) Badge { Text(stringResource(R.string.dynamic_playlist)) }
        audioFormat?.let { Badge { Text(it.toString()) } }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CoverScreenButtons(viewModel: CurrentSongViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isDynamicPlaylistActive by viewModel.isDynamicPlaylistActive.collectAsStateWithLifecycle(false)
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val randomState by viewModel.randomState.collectAsStateWithLifecycle()
    val repeatState by viewModel.repeatState.collectAsStateWithLifecycle()
    val streamingUrl by viewModel.streamingUrl.collectAsStateWithLifecycle(null)

    val filterChipColors = FilterChipDefaults.filterChipColors(
        labelColor = LocalContentColor.current.copy(0.5f),
        iconColor = LocalContentColor.current.copy(0.5f),
        selectedLabelColor = LocalContentColor.current,
        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
    )

    FlowRow(
        modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.Center,
    ) {
        FilterChip(
            shape = ShapeDefaults.ExtraSmall,
            selected = repeatState,
            colors = filterChipColors,
            onClick = { viewModel.toggleRepeatState() },
            label = { Text(stringResource(R.string.repeat)) },
            leadingIcon = { Icon(Icons.Sharp.Repeat, null) }
        )
        FilterChip(
            shape = ShapeDefaults.ExtraSmall,
            selected = randomState,
            colors = filterChipColors,
            onClick = { viewModel.toggleRandomState() },
            label = { Text(stringResource(R.string.shuffle)) },
            leadingIcon = { Icon(Icons.Sharp.Shuffle, null) }
        )
        FilterChip(
            shape = ShapeDefaults.ExtraSmall,
            selected = isStreaming,
            colors = filterChipColors,
            onClick = {
                viewModel.toggleStream { started ->
                    viewModel.addMessage(
                        if (started) {
                            streamingUrl?.let { context.getString(R.string.streaming_from_x_started, it) }
                            ?: context.getString(R.string.streaming_started)
                        } else context.getString(R.string.streaming_stopped)
                    )
                }
            },
            label = { Text(stringResource(R.string.stream)) },
            leadingIcon = { Icon(Icons.Sharp.Headphones, null) }
        )
    }
    if (isDynamicPlaylistActive) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SmallOutlinedButton(
                onClick = { viewModel.deactivateDynamicPlaylist() },
                text = stringResource(R.string.deactivate_dynamic_playlist),
                height = 32.dp,
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
