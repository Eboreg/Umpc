package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode

@Composable
fun SmallSongRowContent(
    modifier: Modifier = Modifier,
    song: MPDSong,
    discNumber: Int? = null,
    isCurrentSong: Boolean,
    showArtist: Boolean = false,
    showYear: Boolean = true,
    playerState: PlayerState?,
    onPlayPauseClick: () -> Unit,
) {
    val durationAndYear = listOfNotNull(
        song.duration?.formatDuration(),
        if (showYear) song.year?.toString() else null,
    )
    val numbers =
        if (discNumber != null && song.trackNumber != null) "${discNumber}-${song.trackNumber}"
        else if (song.trackNumber != null) song.trackNumber.toString()
        else null

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 10.dp),
    ) {
        numbers?.let {
            Text(
                text = it,
                modifier = Modifier.width(35.dp).padding(end = 5.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showArtist) {
                Text(
                    text = song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (isInLandscapeMode()) {
            Text(text = durationAndYear.joinToString(" • "), fontSize = 12.sp)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                durationAndYear.forEach {
                    Text(text = it, fontSize = 12.sp, textAlign = TextAlign.End)
                }
            }
        }
        IconButton(onClick = onPlayPauseClick) {
            if (isCurrentSong && playerState == PlayerState.PLAY)
                Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
            else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
        }
    }
}