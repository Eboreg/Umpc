package us.huseli.umpc.mpd.response

class MPDBatchMapResponse(val commandResponses: List<MPDMapResponse>) : MPDBaseResponse() {
    val successCount = commandResponses.filter { it.isSuccess }.size
    val errorCount = commandResponses.filter { !it.isSuccess }.size

    override fun toString() =
        "${javaClass.simpleName}[status=$status, error=$error, commandResponses=$commandResponses]"
}
