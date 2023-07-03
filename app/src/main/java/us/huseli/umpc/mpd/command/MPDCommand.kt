package us.huseli.umpc.mpd.command

import android.util.Log
import androidx.annotation.WorkerThread
import us.huseli.umpc.data.MPDResponse

@WorkerThread
open class MPDCommand(
    command: String,
    args: Collection<String> = emptyList(),
    onFinish: ((MPDResponse) -> Unit)? = null,
) : MPDBaseCommand(command, args, onFinish) {
    override suspend fun getResponse(): MPDResponse {
        var line: String?
        val responseMap = mutableMapOf<String, String>()
        val responseList = mutableListOf<Pair<String, String>>()
        var status = MPDResponse.Status.EMPTY
        var isFinished = false
        var error: String? = null

        Log.i(javaClass.simpleName, "START $this")

        writeLine(getCommand(command, args))

        do {
            line = readLine()
            if (line != null) {
                if (line == "OK") {
                    status = MPDResponse.Status.OK
                    isFinished = true
                } else if (line.startsWith("ACK ")) {
                    isFinished = true
                    status = MPDResponse.Status.ERROR
                    error = line.substring(4)
                } else if (RESPONSE_REGEX.matches(line)) {
                    parseResponseLine(line)?.let {
                        responseMap.plusAssign(it)
                        responseList.add(it)
                    }
                }
            } else {
                status = MPDResponse.Status.EMPTY
                error = "Empty response"
                isFinished = true
            }
        } while (!isFinished)

        val response = MPDResponse(status, error, responseMap = responseMap, responseList = responseList)
        Log.println(
            if (response.isSuccess) Log.INFO else Log.ERROR,
            javaClass.simpleName,
            "FINISH $this, returning $response"
        )
        return response
    }
}
