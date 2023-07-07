package us.huseli.umpc.data

data class MPDAudioFormat(
    val sampleRate: Int,
    val bitDepth: Int,
    val channels: Int,
) {
    override fun toString() = "${sampleRate}/${bitDepth}/${if (channels == 1) "mono" else "stereo"}"
}

fun CharSequence.toMPDAudioFormat(): MPDAudioFormat? {
    val l = split(':')

    return try {
        MPDAudioFormat(
            sampleRate = l[0].toInt(),
            bitDepth = l[1].toInt(),
            channels = l[2].toInt(),
        )
    } catch (e: Exception) {
        null
    }
}
