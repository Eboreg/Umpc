package us.huseli.umpc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAudioFormat
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.CurrentSongViewModel
import kotlin.math.roundToInt

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
    bitrate: Int?,
    audioFormat: MPDAudioFormat?,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        bitrate?.let { Badge { Text(stringResource(R.string.x_kbps, it)) } }
        audioFormat?.let { Badge { Text(it.toString()) } }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
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
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val randomState by viewModel.randomState.collectAsStateWithLifecycle()
    val repeatState by viewModel.repeatState.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val screenHeightDp = LocalContext.current.resources.configuration.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }
    val anchors = mapOf(0f to "show", screenHeightPx to "hide")
    val landscape = isInLandscapeMode()

    val filterChipColors = FilterChipDefaults.filterChipColors(
        labelColor = LocalContentColor.current.copy(0.5f),
        iconColor = LocalContentColor.current.copy(0.5f),
        selectedLabelColor = LocalContentColor.current,
        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
    )

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
                thresholds = { _, _ -> FractionalThreshold(0.8f) },
            )
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .offset { IntOffset(0, swipeableState.offset.value.roundToInt()) }
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SimpleResponsiveBlock(
                content1 = {
                    FadingImageBox(
                        // modifier = Modifier.fillMaxWidth().weight(1f),
                        fadeStartY = if (landscape) 0f else 0.5f,
                        verticalSpacing = 5.dp,
                        contentPadding = PaddingValues(0.dp),
                        image = { AlbumArt(imageBitmap = albumArt?.fullImage) },
                        topContent = {
                            VolumeSlider(
                                volume = volume.toFloat(),
                                onVolumeChange = { viewModel.setVolume(it.toInt()) },
                            )
                            CoverScreenSongTechInfo(bitrate, audioFormat)
                        },
                        bottomContent = {
                            CoverScreenSongInfoTexts(currentSong, onGotoAlbumClick, onGotoArtistClick)
                        },
                    )
                },
                content2 = {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        SongProgressSlider(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            elapsed = elapsed ?: 0.0,
                            duration = duration ?: 0.0,
                            playerState = playerState,
                            onManualChange = { viewModel.seek(it) },
                        )

                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                shape = ShapeDefaults.ExtraSmall,
                                selected = repeatState,
                                colors = filterChipColors,
                                onClick = { viewModel.toggleRepeatState() },
                                label = { Text(stringResource(R.string.repeat)) },
                                leadingIcon = {
                                    Icon(Icons.Sharp.Repeat, null)
                                }
                            )
                            FilterChip(
                                shape = ShapeDefaults.ExtraSmall,
                                selected = randomState,
                                colors = filterChipColors,
                                onClick = { viewModel.toggleRandomState() },
                                label = { Text(stringResource(R.string.shuffle)) },
                                leadingIcon = {
                                    Icon(Icons.Sharp.Shuffle, null)
                                }
                            )
                            FilterChip(
                                shape = ShapeDefaults.ExtraSmall,
                                selected = isStreaming,
                                colors = filterChipColors,
                                onClick = { viewModel.toggleStream() },
                                label = { Text(stringResource(R.string.stream)) },
                                leadingIcon = {
                                    Icon(Icons.Sharp.Headphones, null)
                                }
                            )
                        }

                        PlayerControls(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            playerState = playerState,
                            onPreviousClick = { viewModel.previousOrRestart() },
                            onPlayPauseClick = { viewModel.playOrPause() },
                            onStopClick = { viewModel.stop() },
                            onNextClick = { viewModel.next() },
                            onForwardClick = { viewModel.seekRelative(10.0) },
                            onReverseClick = { viewModel.seekRelative(-10.0) },
                        )
                    }
                },
            )
        }
    }
}
