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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import us.huseli.umpc.R
import us.huseli.umpc.compose.AddToPlaylistDialog
import us.huseli.umpc.compose.LargeSongRowList
import us.huseli.umpc.compose.utils.SmallOutlinedButton
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.QueueViewModel
import kotlin.math.max

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onAddSongToPlaylistClick: (MPDSong) -> Unit,
    onGotoQueueClick: () -> Unit,
    onGotoPlaylistClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val activeDynamicPlaylist by viewModel.activeDynamicPlaylist.collectAsStateWithLifecycle()
    val connectedServer by viewModel.connectedServer.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentSongPosition by viewModel.currentSongPosition.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var totalDuration by rememberSaveable { mutableDoubleStateOf(0.0) }
    var isSubmenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }

    val scrollToCurrent: () -> Unit = {
        scope.launch { currentSongPosition?.let { viewModel.listState.scrollToItem(max(0, it - 1)) } }
    }

    val addRemovedSongsMessage: (Int) -> Unit = { songCount ->
        viewModel.addMessage(
            SnackbarMessage(
                message = context.resources.getQuantityString(
                    R.plurals.removed_x_songs_from_queue,
                    songCount,
                    songCount
                ),
                actionLabel = context.getString(R.string.undo),
                onActionPerformed = { viewModel.undoRemoveSongs() },
            )
        )
    }

    LaunchedEffect(Unit) {
        scrollToCurrent()
    }

    LaunchedEffect(queue) {
        totalDuration = queue.mapNotNull { it.duration }.sum()
    }

    if (isAddToPlaylistDialogOpen) {
        AddToPlaylistDialog(
            title = stringResource(R.string.queue).lowercase(),
            playlists = playlists,
            allowExistingPlaylist = connectedServer?.protocolVersion?.let { it >= MPDVersion("0.24.0") } == true,
            onConfirm = {
                viewModel.addQueueToPlaylist(it) { response ->
                    if (response.isSuccess) viewModel.addMessage(
                        SnackbarMessage(
                            message = context.getString(R.string.queue_was_added_to_playlist),
                            actionLabel = context.getString(R.string.go_to_playlist),
                            onActionPerformed = { onGotoPlaylistClick(it) },
                        )
                    )
                    else response.error?.let { error -> viewModel.addError(error) }
                }
                isAddToPlaylistDialogOpen = false
            },
            onCancel = { isAddToPlaylistDialogOpen = false },
        )
    }

    LargeSongRowList(
        modifier = modifier,
        viewModel = viewModel,
        songs = queue,
        listState = viewModel.listState,
        currentSong = currentSong,
        playerState = playerState,
        reorderable = true,
        removable = true,
        showSongPositions = true,
        onGotoArtistClick = onGotoArtistClick,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoPlaylistClick = onGotoPlaylistClick,
        onGotoQueueClick = onGotoQueueClick,
        onAddSongToPlaylistClick = onAddSongToPlaylistClick,
        onMoveSong = { from, to -> viewModel.moveSong(from, to) },
        onRemoveSong = { song ->
            viewModel.removeSong(song)
            addRemovedSongsMessage(1)
        },
        onRemoveSelectedSongs = {
            val songCount = viewModel.selectedSongs.value.size
            viewModel.removeSelectedSongs()
            addRemovedSongsMessage(songCount)
        },
        subMenu = {
            QueueScreenSubMenu(
                isSubmenuExpanded = isSubmenuExpanded,
                currentSongPosition = currentSongPosition,
                activeDynamicPlaylist = activeDynamicPlaylist,
                queueSize = queue.size,
                totalDuration = totalDuration,
                onToggleExpandedClick = { isSubmenuExpanded = !isSubmenuExpanded },
                onScrollToCurrentClick = scrollToCurrent,
                onClearQueueClick = {
                    viewModel.clearQueue { viewModel.addMessage(context.getString(R.string.the_queue_was_cleared)) }
                },
                onDeactivateDynamicPlaylistClick = { viewModel.deactivateDynamicPlaylist() },
                onSaveToPlaylistClick = { isAddToPlaylistDialogOpen = true },
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
    onSaveToPlaylistClick: () -> Unit,
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
                            .padding(top = if (isInLandscapeMode()) 10.dp else 0.dp, bottom = 5.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (currentSongPosition != null) {
                            SmallOutlinedButton(
                                modifier = Modifier.padding(bottom = 5.dp),
                                onClick = onScrollToCurrentClick,
                                text = stringResource(R.string.scroll_to_current_song),
                            )
                        }
                        if (activeDynamicPlaylist == null) {
                            SmallOutlinedButton(
                                modifier = Modifier.padding(bottom = 5.dp),
                                onClick = onClearQueueClick,
                                text = stringResource(R.string.clear_queue),
                            )
                        }
                        if (activeDynamicPlaylist != null) {
                            SmallOutlinedButton(
                                modifier = Modifier.padding(bottom = 5.dp),
                                onClick = onDeactivateDynamicPlaylistClick,
                                text = stringResource(R.string.deactivate_dynamic_playlist),
                            )
                        }
                        SmallOutlinedButton(
                            modifier = Modifier.padding(bottom = 5.dp),
                            onClick = onSaveToPlaylistClick,
                            text = stringResource(R.string.save_queue_to_playlist),
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(top = if (isInLandscapeMode()) 10.dp else 0.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalArrangement = Arrangement.Center,
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
