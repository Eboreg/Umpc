package us.huseli.umpc.data

data class MPDVersion(private val value: String = "0.0.0") : Comparable<MPDVersion> {
    private val major = value.split('.')[0].toInt()
    private val minor = value.split('.')[1].toInt()
    private val patch = value.split('.').getOrNull(2)?.toInt()

    override fun compareTo(other: MPDVersion): Int =
        if (other.major != major) major - other.major
        else if (other.minor != minor) minor - other.minor
        else (patch ?: 0) - (other.patch ?: 0)

    override fun toString() = value
}
