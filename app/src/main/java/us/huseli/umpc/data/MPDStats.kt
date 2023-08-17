package us.huseli.umpc.data

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class MPDStats(
    val artists: Int?,
    val albums: Int?,
    val songs: Int?,
    val uptime: Duration?,
    val dbPlaytime: Duration?,
    val dbUpdate: Instant?,
    val playtime: Duration?,
)

fun Map<String, String>.toMPDStats() = MPDStats(
    artists = this["artists"]?.toInt(),
    albums = this["albums"]?.toInt(),
    songs = this["songs"]?.toInt(),
    uptime = this["uptime"]?.toLong()?.toDuration(DurationUnit.SECONDS),
    dbPlaytime = this["db_playtime"]?.toLong()?.toDuration(DurationUnit.SECONDS),
    dbUpdate = this["db_update"]?.toLong()?.let { Instant.ofEpochSecond(it) },
    playtime = this["playtime"]?.toLong()?.toDuration(DurationUnit.SECONDS),
)
