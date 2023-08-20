package us.huseli.umpc.data

data class MPDError(private val code: Int, val commandIdx: Int, val command: String, val message: String) {
    enum class Type(val code: Int) {
        NOT_LIST(1),
        ARG(2),
        PASSWORD(3),
        PERMISSION(4),
        UNKNOWN(5),
        NO_EXIST(50),
        PLAYLIST_MAX(51),
        SYSTEM(52),
        PLAYLIST_LOAD(53),
        UPDATE_ALREADY(54),
        PLAYER_SYNC(55),
        EXIST(56),
    }

    val type: Type? = Type.values().firstOrNull { it.code == code }

    override fun toString() =
        "MPDError[code=$code, commandIdx=$commandIdx, command=$command, type=$type, message=$message]"
}

fun String.toMPDError(): MPDError? =
    Regex("^ACK \\[(\\d+)@(\\d+)] \\{(.*)\\} (.*)$").find(this)?.groupValues?.let { values ->
        MPDError(
            code = values[1].toInt(),
            commandIdx = values[2].toInt(),
            command = values[3],
            message = values[4],
        )
    }
