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

    val type: Type = Type.values().first { it.code == code }

    companion object {
        private val ACK_REGEX = Regex("^ACK \\[(\\d+)@(\\d+)] \\{(.*)\\} (.*)$")

        fun fromString(value: String) = ACK_REGEX.find(value)?.groupValues?.let { values ->
            MPDError(
                code = values[1].toInt(),
                commandIdx = values[2].toInt(),
                command = values[3],
                message = values[4],
            )
        }
    }
}
