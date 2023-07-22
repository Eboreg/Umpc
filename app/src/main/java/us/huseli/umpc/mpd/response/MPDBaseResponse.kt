package us.huseli.umpc.mpd.response

abstract class MPDBaseResponse {
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
        return this as RT
    }

    override fun toString() = "${javaClass.simpleName}[status=$status, error=$error]"

    companion object {
        val responseRegex = Regex("^([^:]*): (.*)$")

        fun parseResponseLine(line: String): Pair<String, String>? =
            responseRegex.find(line)?.groupValues?.let { if (it.size == 3) it[1] to it[2] else null }
    }
}
