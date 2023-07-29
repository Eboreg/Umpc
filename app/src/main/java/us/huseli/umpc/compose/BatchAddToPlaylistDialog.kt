package us.huseli.umpc.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.repository.SnackbarMessage

@Composable
fun BatchAddToPlaylistDialog(
    modifier: Modifier = Modifier,
    itemType: AddToPlaylistItemType,
    itemCount: Int,
    playlists: List<MPDPlaylist>,
    addFunction: (String, (MPDBatchMapResponse) -> Unit) -> Unit,
    addMessage: (SnackbarMessage) -> Unit,
    addError: (SnackbarMessage) -> Unit,
    onGotoPlaylistClick: (String) -> Unit,
    closeDialog: () -> Unit,
) {
    val context = LocalContext.current

    AddToPlaylistDialog(
        modifier = modifier,
        title = when (itemType) {
            AddToPlaylistItemType.SONG -> pluralStringResource(R.plurals.x_songs, itemCount, itemCount)
            AddToPlaylistItemType.ALBUM -> pluralStringResource(R.plurals.x_albums, itemCount, itemCount)
        },
        playlists = playlists,
        onConfirm = {
            addFunction(it) { response ->
                if (response.successCount == 0 && response.errorCount > 0) addError(
                    SnackbarMessage(
                        message = context.resources.getQuantityString(
                            when (itemType) {
                                AddToPlaylistItemType.SONG -> R.plurals.add_songs_playlist_fail
                                AddToPlaylistItemType.ALBUM -> R.plurals.add_albums_playlist_fail
                            },
                            response.errorCount,
                            response.errorCount
                        ),
                    )
                )
                else if (response.successCount > 0 && response.errorCount == 0) addMessage(
                    SnackbarMessage(
                        message = context.resources.getQuantityString(
                            when (itemType) {
                                AddToPlaylistItemType.SONG -> R.plurals.add_songs_playlist_success
                                AddToPlaylistItemType.ALBUM -> R.plurals.add_albums_playlist_success
                            },
                            response.successCount,
                            response.successCount
                        ),
                        actionLabel = context.getString(R.string.go_to_playlist),
                        onActionPerformed = { onGotoPlaylistClick(it) },
                    )
                )
                else addMessage(
                    SnackbarMessage(
                        message = context.getString(
                            when (itemType) {
                                AddToPlaylistItemType.SONG -> R.string.add_songs_playlist_success_and_fail
                                AddToPlaylistItemType.ALBUM -> R.string.add_albums_playlist_success_and_fail
                            },
                            response.successCount,
                            response.errorCount
                        ),
                        actionLabel = context.getString(R.string.go_to_playlist),
                        onActionPerformed = { onGotoPlaylistClick(it) },
                    )
                )
            }
            closeDialog()
        },
        onCancel = closeDialog,
    )
}