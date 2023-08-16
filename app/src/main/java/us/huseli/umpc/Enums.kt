package us.huseli.umpc

enum class ContentScreen { DEBUG, QUEUE, LIBRARY, SETTINGS, SEARCH, PLAYLISTS, NONE }

enum class SingleState { OFF, ON, ONESHOT }

enum class ConsumeState { OFF, ON, ONESHOT }

enum class PlayerState { UNKNOWN, PLAY, STOP, PAUSE }

enum class LibraryGrouping { ARTIST, ALBUM }

enum class ImageRequestType { FULL, THUMBNAIL, BOTH }

enum class LibrarySearchType { ARTIST, ALBUM, NONE }

enum class PlaylistType { STORED, DYNAMIC }

enum class AddToPlaylistItemType { SONG, ALBUM }

enum class MPDServerCapability {
    SAVE_APPEND_REPLACE,
    NEW_FILTER_SYNTAX,
    PLAYLISTMOVE_RANGE,
    CONSUME_ONESHOT,
    SEARCHADD_RELATIVE_POSITION,
    SEARCHADDPL_POSITION,
    PLAYLISTDELETE_RANGE,
    PLAYLISTADD_POSITION,
    ADD_POSITION,
    LOAD_POSITION,
    SEARCHADD_POSITION,
    ADDID_RELATIVE_POSITION,
    GETVOL,
    BINARYLIMIT,
    SINGLE_ONESHOT,
    STATUS_DURATION,
    RANGEID,
    REPLAY_GAIN_MODE,
    STATUS_ELAPSED,
    STICKERS,
    PLAYLISTINFO_RANGE,
    MOVE_RANGE,
    SINGLE,
    CONSUME,
    STATUS_NEXTSONG,
    STATUS_NEXTSONGID,
    IDLE,
}

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
