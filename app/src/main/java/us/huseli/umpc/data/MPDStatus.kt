package us.huseli.umpc.data

import android.util.Log
import us.huseli.umpc.ConsumeState
import us.huseli.umpc.Logger
import us.huseli.umpc.PlayerState
import us.huseli.umpc.SingleState
import us.huseli.umpc.toConsumeState
import us.huseli.umpc.toPlayerState
import us.huseli.umpc.toSingleState

data class MPDStatus(
    val volume: Int? = null,
    val repeat: Boolean? = null,
    val random: Boolean? = null,
    val single: SingleState? = null,
    val consume: ConsumeState? = null,
    val partition: String? = null,
    val queueVersion: Int? = null,
    val queueLength: Int? = null,
    val mixRampDb: Int? = null,
    val playerState: PlayerState? = null,
    val currentSongPosition: Int? = null,
    val currentSongId: Int? = null,
    val nextSongPosition: Int? = null,
    val nextSongId: Int? = null,
    // Below: only returned when state is not stop
    val currentSongElapsed: Double? = null,
    val currentSongDuration: Double? = null,
    val bitrate: Int? = null,
    val audioFormat: MPDAudioFormat? = null,
    // Don't know when these are returned:
    val xfade: Int? = null,
    val mixRampDelay: Int? = null,
    val error: String? = null,
    // Only when a DB update is in progress:
    val dbUpdateId: Int? = null,
)

fun Map<String, String>.toMPDStatus() = try {
    MPDStatus(
        volume = this["volume"]?.toInt(),
        repeat = this["repeat"]?.let { it == "1" },
        random = this["random"]?.let { it == "1" },
        single = this["single"]?.toSingleState(),
        consume = this["consume"]?.toConsumeState(),
        partition = this["partition"],
        queueVersion = this["playlist"]?.toInt(),
        queueLength = this["playlistlength"]?.toInt(),
        mixRampDb = this["mixrampdb"]?.toInt(),
        playerState = this["state"]?.toPlayerState(),
        currentSongPosition = this["song"]?.toInt(),
        currentSongId = this["songid"]?.toInt(),
        nextSongPosition = this["nextsong"]?.toInt(),
        nextSongId = this["nextsongid"]?.toInt(),
        currentSongElapsed = this["elapsed"]?.toDouble() ?: this["time"]?.replace(':', '.')?.toDouble(),
        currentSongDuration = this["duration"]?.toDouble(),
        bitrate = this["bitrate"]?.toInt()?.takeIf { it > 0 },
        audioFormat = this["audio"]?.toMPDAudioFormat(),
        xfade = this["xfade"]?.toInt(),
        mixRampDelay = this["mixrampdelay"]?.toInt(),
        error = this["error"],
        dbUpdateId = this["updating_db"]?.toInt(),
    )
} catch (e: Exception) {
    Logger.log("MPDStatus", "fromMap: $e", Log.ERROR)
    null
}
