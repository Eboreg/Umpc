package us.huseli.umpc.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ExpandLess
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import us.huseli.umpc.R
import us.huseli.umpc.compose.LargeSongRowList
import us.huseli.umpc.compose.utils.SmallOutlinedButton
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.QueueViewModel
import kotlin.math.max

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onAddSongToPlaylistClick: (MPDSong) -> Unit,
) {
    val activeDynamicPlaylist by viewModel.activeDynamicPlaylist.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentSongPosition by viewModel.currentSongPosition.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var totalDuration by rememberSaveable { mutableStateOf(0.0) }
    var isSubmenuExpanded by rememberSaveable { mutableStateOf(false) }
    val queueClearedMsg = stringResource(R.string.the_queue_was_cleared)

    val scrollToCurrent: () -> Unit = {
        scope.launch { currentSongPosition?.let { viewModel.listState.scrollToItem(max(0, it - 1)) } }
    }

    LaunchedEffect(queue) {
        scrollToCurrent()
        totalDuration = queue.mapNotNull { it.duration }.sum()
    }

    LargeSongRowList(
        modifier = modifier,
        viewModel = viewModel,
        songs = queue,
        listState = viewModel.listState,
        currentSong = currentSong,
        playerState = playerState,
        reorderable = true,
        showSongPositions = true,
        onGotoArtistClick = onGotoArtistClick,
        onGotoAlbumClick = onGotoAlbumClick,
        onAddSongToPlaylistClick = onAddSongToPlaylistClick,
        onMoveSong = { from, to -> viewModel.moveSong(from, to) },
        subMenu = {
            QueueScreenSubMenu(
                isSubmenuExpanded = isSubmenuExpanded,
                currentSongPosition = currentSongPosition,
                activeDynamicPlaylist = activeDynamicPlaylist,
                queueSize = queue.size,
                totalDuration = totalDuration,
                onToggleExpandedClick = { isSubmenuExpanded = !isSubmenuExpanded },
                onScrollToCurrentClick = scrollToCurrent,
                onClearQueueClick = { viewModel.clearQueue { viewModel.addMessage(queueClearedMsg) } },
                onDeactivateDynamicPlaylistClick = { viewModel.deactivateDynamicPlaylist() },
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QueueScreenSubMenu(
    modifier: Modifier = Modifier,
    isSubmenuExpanded: Boolean,
    currentSongPosition: Int?,
    activeDynamicPlaylist: DynamicPlaylist?,
    queueSize: Int,
    totalDuration: Double,
    onToggleExpandedClick: () -> Unit,
    onScrollToCurrentClick: () -> Unit,
    onClearQueueClick: () -> Unit,
    onDeactivateDynamicPlaylistClick: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpandedClick)
    ) {
        Box {
            Column(modifier = modifier.fillMaxWidth()) {
                if (isSubmenuExpanded) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .padding(top = if (isInLandscapeMode()) 10.dp else 0.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (currentSongPosition != null) {
                            SmallOutlinedButton(
                                onClick = onScrollToCurrentClick,
                                text = stringResource(R.string.scroll_to_current_song),
                            )
                        }
                        if (activeDynamicPlaylist == null) {
                            SmallOutlinedButton(
                                onClick = onClearQueueClick,
                                text = stringResource(R.string.clear_queue),
                            )
                        }
                        if (activeDynamicPlaylist != null) {
                            SmallOutlinedButton(
                                onClick = onDeactivateDynamicPlaylistClick,
                                text = stringResource(R.string.deactivate_dynamic_playlist),
                            )
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(top = if (isInLandscapeMode()) 10.dp else 0.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Badge { Text(pluralStringResource(R.plurals.x_songs, queueSize, queueSize)) }
                    if (activeDynamicPlaylist != null) {
                        Badge {
                            Text(
                                if (isSubmenuExpanded) stringResource(
                                    R.string.active_dynamic_playlist_x,
                                    activeDynamicPlaylist.toString()
                                )
                                else stringResource(R.string.dynamic_playlist)
                            )
                        }
                    }
                    Badge { Text(totalDuration.formatDuration()) }
                }
            }
            Icon(
                imageVector = if (isSubmenuExpanded) Icons.Sharp.ExpandLess else Icons.Sharp.ExpandMore,
                contentDescription = null,
                modifier = Modifier.height(16.dp).align(Alignment.BottomCenter),
            )
        }
    }
}
