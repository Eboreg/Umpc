package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Suppress("unused")
enum class MPDServerCapability(val fromVersion: String) {
    ADDID_RELATIVE_POSITION("0.23"),
    ADD_POSITION("0.23.3"),
    BINARYLIMIT("0.22.4"),
    CONSUME("0.15"),
    CONSUME_ONESHOT("0.24"),
    GETVOL("0.23"),
    IDLE("0.14"),
    LOAD_POSITION("0.23.1"),
    MOVE_RANGE("0.15"),
    NEW_FILTER_SYNTAX("0.21"),
    PLAYLISTADD_POSITION("0.23.3"),
    PLAYLISTDELETE_RANGE("0.23.3"),
    PLAYLISTINFO_RANGE("0.15"),
    PLAYLISTMOVE_RANGE("0.24"),
    RANGEID("0.19"),
    REPLAY_GAIN_MODE("0.16"),
    SAVE_APPEND_REPLACE("0.24"),
    SEARCHADDPL_POSITION("0.23.4"),
    SEARCHADD_POSITION("0.23"),
    SEARCHADD_RELATIVE_POSITION("0.23.5"),
    SINGLE("0.15"),
    SINGLE_ONESHOT("0.21"),
    STATUS_DURATION("0.20"),
    STATUS_ELAPSED("0.16"),
    STATUS_NEXTSONG("0.15"),
    STATUS_NEXTSONGID("0.15"),
    STICKERS("0.15"),
}

@Parcelize
open class MPDServer(val hostname: String, val port: Int, val protocolVersion: MPDVersion? = null) : Parcelable {
    fun hasCapability(capability: MPDServerCapability) =
        protocolVersion?.hasCapability(capability) ?: false

    override fun toString() = "$hostname:$port"
    override fun equals(other: Any?) = other is MPDServer && other.hostname == hostname && other.port == port
    override fun hashCode(): Int = 31 * hostname.hashCode() + port

    companion object {
        fun fromString(value: String): MPDServer = value.split(":").let { MPDServer(it[0], it[1].toInt()) }
    }
}


class MPDServerCredentials(
    hostname: String,
    port: Int,
    val streamingUrl: String? = null,
    val password: String? = null,
) : MPDServer(hostname, port) {
    override fun equals(other: Any?) =
        other is MPDServerCredentials &&
        super.equals(other) &&
        other.password == password &&
        other.streamingUrl == streamingUrl

    override fun hashCode(): Int =
        31 * (31 * super.hashCode() + (streamingUrl?.hashCode() ?: 0)) + (password?.hashCode() ?: 0)
}
