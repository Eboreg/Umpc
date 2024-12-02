package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowDropDown
import androidx.compose.material.icons.sharp.ArrowRight
import androidx.compose.material.icons.sharp.Folder
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDDirectory
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.DirectoryViewModel

@Composable
fun LibraryScreenDirectorySection(
    modifier: Modifier = Modifier,
    viewModel: DirectoryViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
) {
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val openDirectories by viewModel.openDirectories.collectAsStateWithLifecycle()
    val rootContents by viewModel.getDirectoryContents("").collectAsStateWithLifecycle()

    LazyColumn(state = listState, modifier = modifier) {
        items(rootContents.directories) { dir ->
            val isOpen = openDirectories.contains(dir.path)

            DirectoryRow(
                directory = dir,
                isOpen = isOpen,
                level = 0,
                onClick = { viewModel.toggleDirectoryOpen(dir.path) },
            )
            if (isOpen) {
                DirectoryContents(
                    viewModel = viewModel,
                    path = dir.path,
                    level = 1,
                    currentSongFilename = currentSongFilename,
                    playerState = playerState,
                    openDirectories = openDirectories,
                    onDirectoryClick = { viewModel.toggleDirectoryOpen(it.path) },
                    listScope = this@LazyColumn,
                )
            }
        }
    }
}

@Composable
fun DirectoryContents(
    viewModel: DirectoryViewModel,
    path: String,
    level: Int,
    currentSongFilename: String?,
    playerState: PlayerState,
    openDirectories: Set<String>,
    onDirectoryClick: (MPDDirectory) -> Unit,
    listScope: LazyListScope,
) {
    val directory by viewModel.getDirectoryContents(path).collectAsStateWithLifecycle()

    if (!directory.contentsLoaded) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .padding(start = 10.dp + (20.dp * level), end = 10.dp),
            content = { Text(text = stringResource(R.string.loading)) },
        )
    }

    directory.directories.forEach { subdir ->
        val isOpen = openDirectories.contains(subdir.path)

        listScope.item {
            DirectoryRow(
                directory = subdir,
                isOpen = isOpen,
                level = level,
                onClick = { onDirectoryClick(subdir) },
            )
        }
        if (isOpen) {
            DirectoryContents(
                viewModel = viewModel,
                path = subdir.path,
                level = level + 1,
                currentSongFilename = currentSongFilename,
                playerState = playerState,
                openDirectories = openDirectories,
                onDirectoryClick = onDirectoryClick,
                listScope = listScope,
            )
        }
    }
    directory.songs.forEach { song ->
        listScope.item {
            SongFileRow(song = song, level = level, onPlayClick = { viewModel.playDirectory(directory, song) })
        }
    }
}

@Composable
fun DirectoryRow(
    directory: MPDDirectory,
    isOpen: Boolean,
    level: Int,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(start = 10.dp + (20.dp * level))
            .padding(vertical = 10.dp),
    ) {
        if (isOpen) Icon(Icons.Sharp.ArrowDropDown, null)
        else Icon(Icons.Sharp.ArrowRight, null)
        Icon(Icons.Sharp.Folder, null)
        Text(text = directory.name)
    }
}

@Composable
fun SongFileRow(
    song: MPDSong,
    level: Int,
    onPlayClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp + (20.dp * level))
            .padding(vertical = 10.dp),
    ) {
        Icon(Icons.Sharp.MusicNote, null)
        Text(text = song.basename, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Sharp.PlayArrow,
            contentDescription = stringResource(R.string.play),
            modifier = Modifier.clickable(onClick = onPlayClick),
        )
    }
}
