package us.huseli.umpc.mpd.response

import android.util.Log
import us.huseli.umpc.data.MPDError

abstract class BaseMPDResponse {
    enum class Status { PENDING, OK, ERROR_MPD, ERROR_OTHER, EMPTY_RESPONSE }

    protected val _responseLines = mutableListOf<String>()

    var status = Status.PENDING
        private set
    var exception: Throwable? = null
        private set
    var error: String? = null
        private set
    var mpdError: MPDError? = null
        private set

    @Suppress("BooleanMethodIsAlwaysInverted")
    val isSuccess: Boolean
        get() = status == Status.OK

    open val logLevel: Int
        get() = if (isSuccess) Log.INFO else Log.ERROR

    @Suppress("UNCHECKED_CAST")
    open fun <RT : BaseMPDResponse> finish(
        status: Status,
        exception: Throwable? = null,
        mpdError: MPDError? = null,
        error: String? = null,
    ): RT {
        this.status = status
        this.exception = exception
        this.error = error ?: exception?.toString() ?: mpdError?.message
        this.mpdError = mpdError
        return this as RT
    }

    fun putLine(line: String) {
        _responseLines.add(line)
    }

    override fun toString() = "${javaClass.simpleName}[status=$status, error=$error, mpdError=$mpdError]"

    companion object {
        val responseRegex = Regex("^([^:]*): (.*)$")

        fun parseResponseLine(line: String?): Pair<String, String>? =
            if (line != null) responseRegex.find(line)?.groupValues?.let { if (it.size == 3) it[1] to it[2] else null }
            else null
    }
}
