package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDArtistWithAlbums

@Composable
fun ArtistSection(
    modifier: Modifier = Modifier,
    artist: MPDArtistWithAlbums,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        AutoScrollingTextLine(text = artist.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = pluralStringResource(R.plurals.x_albums, artist.albums.size, artist.albums.size),
            style = MaterialTheme.typography.labelMedium,
        )
    }

    if (isExpanded) {
        CompositionLocalProvider(
            LocalAbsoluteTonalElevation provides LocalAbsoluteTonalElevation.current + 2.dp,
        ) { Surface { Column { expandedContent() } } }
    }
}
