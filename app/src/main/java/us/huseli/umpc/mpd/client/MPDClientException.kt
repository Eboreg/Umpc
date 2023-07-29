package us.huseli.umpc.mpd.client

class MPDClientException(
    client: MPDBaseClient,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    val clientClass: String = client.javaClass.simpleName
}