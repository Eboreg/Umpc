package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R
import us.huseli.umpc.compose.utils.AutoScrollingTextLine
import us.huseli.umpc.data.MPDArtistWithAlbums
import us.huseli.umpc.isInLandscapeMode

@Composable
fun ArtistRow(
    modifier: Modifier = Modifier,
    artist: MPDArtistWithAlbums,
    padding: PaddingValues = PaddingValues(10.dp),
    onGotoArtistClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onGotoArtistClick() }
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
    }
}
