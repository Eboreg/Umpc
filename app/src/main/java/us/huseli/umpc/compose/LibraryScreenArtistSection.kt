package us.huseli.umpc.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.compose.ListWithAlphabetBar
import us.huseli.umpc.R
import us.huseli.umpc.replaceLeadingJunk
import us.huseli.umpc.viewmodels.LibraryViewModel
import kotlin.math.roundToInt

@Composable
fun LibraryScreenArtistSection(
    onGotoArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val artistLeadingChars by viewModel.artistLeadingChars.collectAsStateWithLifecycle(emptyList())
    val artistsLoaded by viewModel.artistsLoaded.collectAsStateWithLifecycle()

    if (artists.isEmpty()) {
        Text(
            text = stringResource(if (!artistsLoaded) R.string.loading else R.string.no_artists_found_you_may_want_to_add),
            modifier = Modifier.padding(10.dp),
        )
    }

    ListWithAlphabetBar(
        modifier = modifier.fillMaxWidth(),
        characters = artistLeadingChars,
        listState = listState,
        items = artists,
        selector = { it.name.replaceLeadingJunk() },
        minItems = (LocalConfiguration.current.screenHeightDp * 0.042).roundToInt(),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            items(artists, key = { it.name }) { artist ->
                ArtistRow(
                    artist = artist,
                    padding = PaddingValues(10.dp),
                    onGotoArtistClick = { onGotoArtistClick(artist.name) }
                )
                Divider()
            }
        }
    }
}
