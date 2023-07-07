package us.huseli.umpc.mpd.command

import android.util.Log
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.mpd.response.MPDResponse

open class MPDCommand(
    command: String,
    args: Collection<String> = emptyList(),
    onFinish: ((MPDResponse) -> Unit)? = null,
) : MPDBaseCommand(command, args, onFinish), LoggerInterface {
    constructor(command: String, arg: String, onFinish: ((MPDResponse) -> Unit)? = null) :
        this(command, listOf(arg), onFinish)

    override suspend fun getResponse(): MPDResponse {
        var line: String?
        val responseMap = mutableMapOf<String, String>()
        val responseList = mutableListOf<Pair<String, String>>()
        var status = MPDResponse.Status.PENDING
        var isFinished = false
        var error: String? = null

        log("START $this")
        try {
            writeLine(getCommand(command, args))
        } catch (e: Exception) {
            return MPDResponse(status = MPDResponse.Status.ERROR_NET, exception = e)
        }

        do {
            try {
                line = readLine()
            } catch (e: Exception) {
                return MPDResponse(status = MPDResponse.Status.ERROR_NET, exception = e)
            }
            if (line != null) {
                if (line == "OK") {
                    status = MPDResponse.Status.OK
                    isFinished = true
                } else if (line.startsWith("ACK ")) {
                    isFinished = true
                    status = MPDResponse.Status.ERROR_MPD
                    error = line.substring(4)
                } else if (responseRegex.matches(line)) {
                    parseResponseLine(line)?.let {
                        responseMap.plusAssign(it)
                        responseList.add(it)
                    }
                }
            } else {
                status = MPDResponse.Status.EMPTY_RESPONSE
            }
        } while (!isFinished)

        val response =
            MPDResponse(status = status, error = error, responseMap = responseMap, responseList = responseList)
        log("FINISH $this, returning $response", level = if (response.isSuccess) Log.INFO else Log.ERROR)
        return response
    }
}
