package us.huseli.umpc.data

import android.util.Log
import us.huseli.umpc.ConsumeState
import us.huseli.umpc.PlayerState
import us.huseli.umpc.SingleState
import us.huseli.umpc.toConsumeState
import us.huseli.umpc.toPlayerState
import us.huseli.umpc.toSingleState

data class MPDStatus(
    val volume: Int?,
    val repeat: Boolean?,
    val random: Boolean?,
    val single: SingleState?,
    val consume: ConsumeState?,
    val partition: String?,
    val queueVersion: Int?,
    val queueLength: Int?,
    val mixRampDb: Int?,
    val playerState: PlayerState?,
    val currentSongIndex: Int?,
    val currentSongId: Int?,
    val nextSongIndex: Int?,
    val nextSongId: Int?,
    // Below: only returned when state is not stop
    val currentSongElapsed: Double?,
    val currentSongDuration: Double?,
    val bitrate: Int?,
    val audioFormat: MPDAudioFormat?,
    // Don't know when these are returned:
    val xfade: Int?,
    val mixRampDelay: Int?,
    val error: String?,
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
        currentSongIndex = this["song"]?.toInt(),
        currentSongId = this["songid"]?.toInt(),
        nextSongIndex = this["nextsong"]?.toInt(),
        nextSongId = this["nextsongid"]?.toInt(),
        currentSongElapsed = this["elapsed"]?.toDouble(),
        currentSongDuration = this["duration"]?.toDouble(),
        bitrate = this["bitrate"]?.toInt(),
        audioFormat = this["audio"]?.toMPDAudioFormat(),
        xfade = this["xfade"]?.toInt(),
        mixRampDelay = this["mixrampdelay"]?.toInt(),
        error = this["error"],
    )
} catch (e: Exception) {
    Log.e("MPDStatus", "fromMap: $e")
    null
}
