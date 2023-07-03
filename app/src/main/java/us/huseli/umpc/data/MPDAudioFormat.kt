package us.huseli.umpc.data

data class MPDAudioFormat(
    val sampleRate: Int?,
    val bitDepth: Int?,
    val channels: Int?,
) {
    override fun toString() = "${sampleRate ?: "*"}:${bitDepth ?: "*"}:${channels ?: "*"}"
}

fun CharSequence.toMPDAudioFormat(): MPDAudioFormat {
    val l = split(':')

    return MPDAudioFormat(
        sampleRate = l[0].toIntOrNull(),
        bitDepth = l[1].toIntOrNull(),
        channels = l[2].toIntOrNull(),
    )
}
