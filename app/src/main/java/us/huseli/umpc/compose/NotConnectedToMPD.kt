package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R

@Composable
fun NotConnectedToMPD(modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(10.dp)) {
        Text(stringResource(R.string.currently_not_connected_to_an_mpd_server))
    }
}
