package us.huseli.umpc

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.umpc.Constants.NAV_ARG_ALBUM
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.Constants.NAV_ARG_PLAYLIST
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDPlaylist

open class Destination(val route: String)

object AlbumDestination {
    const val routeTemplate = "album/{$NAV_ARG_ARTIST}/{$NAV_ARG_ALBUM}"
    val arguments = listOf(
        navArgument(NAV_ARG_ARTIST) { type = NavType.StringType },
        navArgument(NAV_ARG_ALBUM) { type = NavType.StringType },
    )

    fun route(album: MPDAlbum) = "album/${album.artist}/${album.name}"
}

object ArtistDestination {
    const val routeTemplate = "artist/{$NAV_ARG_ARTIST}"
    val arguments = listOf(
        navArgument(NAV_ARG_ARTIST) { type = NavType.StringType },
    )

    fun route(name: String) = "artist/$name"
}

object PlaylistDetailsDestination {
    const val routeTemplate = "playlist/{$NAV_ARG_PLAYLIST}"
    val arguments = listOf(
        navArgument(NAV_ARG_PLAYLIST) { type = NavType.StringType },
    )

    fun route(playlist: MPDPlaylist) = "playlist/${playlist.name}"
    fun route(playlistName: String) = "playlist/$playlistName"
}

object LibraryDestination : Destination("library")
object QueueDestination : Destination("queue")
object DebugDestination : Destination("debug")
object SettingsDestination : Destination("settings")
object SearchDestination : Destination("search")
object PlaylistListDestination : Destination("playlists")
