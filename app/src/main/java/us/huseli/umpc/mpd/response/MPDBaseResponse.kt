package us.huseli.umpc.mpd.response

import android.util.Log
import us.huseli.umpc.LoggerInterface

abstract class MPDBaseResponse : LoggerInterface {
    enum class Status { PENDING, OK, ERROR_MPD, ERROR_NET, ERROR_OTHER, EMPTY_RESPONSE }

    var status = Status.PENDING
        private set
    var exception: Throwable? = null
        private set
    var error: String? = null
        private set

    val isSuccess: Boolean
        get() = status == Status.OK

    @Suppress("UNCHECKED_CAST")
    open fun <RT : MPDBaseResponse> finish(
        status: Status,
        exception: Throwable? = null,
        error: String? = exception?.toString(),
    ): RT {
        this.status = status
        this.exception = exception
        this.error = error
        log("FINISH $this", level = if (status == Status.OK) Log.INFO else Log.ERROR)
        return this as RT
    }

    override fun toString() =
        "${javaClass.simpleName}[status=$status, error=$error]"
}
