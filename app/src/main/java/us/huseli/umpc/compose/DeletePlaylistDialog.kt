package us.huseli.umpc.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.umpc.R

@Composable
fun DeletePlaylistDialog(
    modifier: Modifier = Modifier,
    name: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(stringResource(R.string.delete_playlist)) },
        onDismissRequest = onCancel,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        text = { Text(stringResource(R.string.delete_playlist_x, name)) }
    )
}
