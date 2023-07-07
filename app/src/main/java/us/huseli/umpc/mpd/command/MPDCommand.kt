package us.huseli.umpc.mpd.command

import android.util.Log
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDResponse

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
        var isSuccess = true
        var isFinished = false
        var error: String? = null

        log("START $this")
        writeLine(getCommand(command, args))

        do {
            line = readLine()
            if (line != null) {
                if (line == "OK") {
                    isFinished = true
                } else if (line.startsWith("ACK ")) {
                    isFinished = true
                    isSuccess = false
                    error = line.substring(4)
                } else if (responseRegex.matches(line)) {
                    parseResponseLine(line)?.let {
                        responseMap.plusAssign(it)
                        responseList.add(it)
                    }
                }
            } else {
                throw MPDCommandEmptyResponseException()
            }
        } while (!isFinished)

        val response = MPDResponse(isSuccess, error, responseMap = responseMap, responseList = responseList)
        log("FINISH $this, returning $response", level = if (response.isSuccess) Log.INFO else Log.ERROR)
        return response
    }
}
