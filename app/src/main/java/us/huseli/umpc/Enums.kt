package us.huseli.umpc

enum class ContentScreen(val value: String) {
    DEBUG("debug"),
    QUEUE("queue"),
    LIBRARY("library"),
    COVER("cover"),
    SETTTINGS("settings"),
    SEARCH("search"),
    NONE("none"),
}

enum class SingleState(val value: String) { OFF("0"), ON("1"), ONESHOT("oneshot") }

enum class ConsumeState(val value: String) { OFF("0"), ON("1"), ONESHOT("oneshot") }

enum class PlayerState(val value: String) { PLAY("play"), STOP("stop"), PAUSE("pause") }

enum class LibraryGrouping { ARTIST, ALBUM }

enum class ImageRequestType { FULL, THUMBNAIL, BOTH }

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
