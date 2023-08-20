package us.huseli.umpc.mpd.client

open class MPDException(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)

class MPDClientException(override val message: String, override val cause: Throwable? = null) :
    MPDException(message, cause)

class MPDAuthException(override val message: String, override val cause: Throwable? = null) :
    MPDException(message, cause)
