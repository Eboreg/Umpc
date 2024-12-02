package us.huseli.umpc.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import us.huseli.umpc.LibraryGrouping
import us.huseli.umpc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenSubMenu(
    grouping: LibraryGrouping,
    isSearchActive: Boolean,
    isConnected: Boolean,
    setGrouping: (LibraryGrouping) -> Unit,
    activateLibrarySearch: (LibraryGrouping) -> Unit,
    deactivateLibrarySearch: () -> Unit,
) {
    FilterChip(
        shape = ShapeDefaults.ExtraSmall,
        selected = grouping == LibraryGrouping.ARTIST,
        onClick = { setGrouping(LibraryGrouping.ARTIST) },
        label = { Text(stringResource(R.string.artists)) },
    )
    FilterChip(
        shape = ShapeDefaults.ExtraSmall,
        selected = grouping == LibraryGrouping.ALBUM,
        onClick = { setGrouping(LibraryGrouping.ALBUM) },
        label = { Text(stringResource(R.string.albums)) },
    )
    FilterChip(
        shape = ShapeDefaults.ExtraSmall,
        selected = grouping == LibraryGrouping.DIRECTORY,
        onClick = { setGrouping(LibraryGrouping.DIRECTORY) },
        label = { Text(stringResource(R.string.directories)) },
    )
    IconToggleButton(
        checked = isSearchActive,
        enabled = isConnected,
        onCheckedChange = {
            if (it) activateLibrarySearch(grouping)
            else deactivateLibrarySearch()
        },
        content = { Icon(Icons.Sharp.Search, stringResource(R.string.search)) },
    )
}