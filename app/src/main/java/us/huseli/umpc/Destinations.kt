package us.huseli.umpc

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.umpc.Constants.NAV_ARG_ALBUM
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.data.MPDSong

open class Destination(val route: String)

object AlbumDestination {
    private const val baseRoute = "album"
    const val routeTemplate = "$baseRoute/{$NAV_ARG_ARTIST}/{$NAV_ARG_ALBUM}"
    val arguments = listOf(
        navArgument(NAV_ARG_ARTIST) { type = NavType.StringType },
        navArgument(NAV_ARG_ALBUM) { type = NavType.StringType },
    )

    fun route(song: MPDSong) = "$baseRoute/${song.albumArtist}/${song.album}"
}

object ArtistDestination {
    private const val baseRoute = "artist"
    const val routeTemplate = "$baseRoute/{$NAV_ARG_ARTIST}"
    val arguments = listOf(
        navArgument(NAV_ARG_ARTIST) { type = NavType.StringType },
    )

    fun route(song: MPDSong) = "$baseRoute/${song.artist}"
}

object QueueDestination : Destination("queue")
object LibraryDestination : Destination("library")
object CoverDestination : Destination("cover")
object DebugDestination : Destination("debug")
object SettingsDestination : Destination("settings")
object SearchDestination : Destination("search")
