package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class MPDVersion(private val value: String = "0.0.0") : Comparable<MPDVersion>, Parcelable {
    @IgnoredOnParcel
    private val major = value.split('.')[0].toInt()
    @IgnoredOnParcel
    private val minor = value.split('.')[1].toInt()
    @IgnoredOnParcel
    private val patch = value.split('.').getOrNull(2)?.toInt()

    fun hasCapability(capability: MPDServerCapability) = this >= MPDVersion(capability.fromVersion)

    override fun compareTo(other: MPDVersion): Int =
        if (other.major != major) major - other.major
        else if (other.minor != minor) minor - other.minor
        else (patch ?: 0) - (other.patch ?: 0)

    override fun toString() = value
}
