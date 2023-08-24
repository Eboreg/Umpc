package us.huseli.umpc

enum class ContentScreen { QUEUE, LIBRARY, SETTINGS, SEARCH, PLAYLISTS, INFO, NONE }

enum class SingleState { OFF, ON, ONESHOT }

enum class ConsumeState { OFF, ON, ONESHOT }

enum class PlayerState { UNKNOWN, PLAY, STOP, PAUSE }

enum class LibraryGrouping { ARTIST, ALBUM }

enum class LibrarySearchType { ARTIST, ALBUM, NONE }

enum class PlaylistType { STORED, DYNAMIC }

enum class AddToPlaylistItemType { SONG, ALBUM }

fun String.toSingleState() = when (this) {
    "0" -> SingleState.OFF
    "1" -> SingleState.ON
    "oneshot" -> SingleState.ONESHOT
    else -> null
}

fun String.toConsumeState() = when (this) {
    "0" -> ConsumeState.OFF
    "1" -> ConsumeState.ON
    "oneshot" -> ConsumeState.ONESHOT
    else -> null
}

fun String.toPlayerState() = when (this) {
    "play" -> PlayerState.PLAY
    "pause" -> PlayerState.PAUSE
    "stop" -> PlayerState.STOP
    else -> null
}
