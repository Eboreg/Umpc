package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MPDServer(val hostname: String, val port: Int) : Parcelable {
    override fun toString() = "$hostname:$port"

    companion object {
        fun fromString(value: String): MPDServer = value.split(":").let { MPDServer(it[0], it[1].toInt()) }
    }
}
