package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowOutward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDArtistWithAlbums
import us.huseli.umpc.isInLandscapeMode

@Composable
fun ArtistRow(
    modifier: Modifier = Modifier,
    artist: MPDArtistWithAlbums,
    padding: PaddingValues = PaddingValues(10.dp),
    onGotoArtistClick: () -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(padding)
    ) {
        if (isInLandscapeMode()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = pluralStringResource(R.plurals.x_albums, artist.albums.size, artist.albums.size),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                AutoScrollingTextLine(text = artist.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = pluralStringResource(R.plurals.x_albums, artist.albums.size, artist.albums.size),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        IconButton(onClick = onGotoArtistClick, modifier = Modifier.width(IntrinsicSize.Min)) {
            Icon(Icons.Sharp.ArrowOutward, stringResource(R.string.go_to_artist))
        }
    }

    if (isExpanded) {
        CompositionLocalProvider(LocalAbsoluteTonalElevation provides LocalAbsoluteTonalElevation.current + 2.dp) {
            Surface { Column { expandedContent() } }
        }
    }
}
