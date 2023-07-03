package us.huseli.umpc.mpd

import android.util.Log
import androidx.annotation.WorkerThread
import us.huseli.umpc.data.MPDResponse

@WorkerThread
class MPDBinaryCommand(
    command: String,
    args: Collection<String> = emptyList(),
    onFinish: ((MPDResponse) -> Unit)? = null,
) : MPDBaseCommand(command, args, onFinish) {
    override suspend fun getResponse(): MPDResponse {
        Log.i(javaClass.simpleName, "START $this")
        val response = getBinaryResponse()
        Log.println(
            if (response.isSuccess) Log.INFO else Log.ERROR,
            javaClass.simpleName,
            "FINISH $this, returning $response"
        )
        return response
    }

    private suspend fun getBinaryResponse(): MPDResponse {
        val responseMap = mutableMapOf<String, String>()
        var binarySize = 0
        var dataToRead = 0
        var binaryData = ByteArray(0)
        var firstRun = true
        var line: String?

        // Outer loop makes server requests until all chunks are fetched:
        while (dataToRead > 0 || firstRun) {
            writeLine(getCommand(command, args.plus((binarySize - dataToRead).toString())))
            line = readLine()

            if (line != null) {
                if (firstRun && line.startsWith("OK")) {
                    return MPDResponse(status = MPDResponse.Status.ERROR, error = "No binary data returned")
                }
                if (line.startsWith("ACK ")) {
                    return MPDResponse(status = MPDResponse.Status.ERROR, error = line.substring(4))
                }
            } else {
                return MPDResponse(status = MPDResponse.Status.EMPTY)
            }

            // Inner loop handles individual server responses:
            while (line != null && !line.startsWith("OK")) {
                parseResponseLine(line)?.also { (key, value) ->
                    responseMap[key] = value
                    if (firstRun && key == "size") {
                        binarySize = value.toInt()
                        binaryData = ByteArray(binarySize)
                        dataToRead = binarySize
                        firstRun = false
                    }
                    if (key == "binary") {
                        val chunkSize = value.toInt()
                        val chunk = readBinary(chunkSize)

                        if (dataToRead - chunkSize < 0) {
                            return MPDResponse(status = MPDResponse.Status.ERROR, error = "Got too much data")
                        } else {
                            System.arraycopy(chunk, 0, binaryData, binarySize - dataToRead, chunkSize)
                            dataToRead -= chunkSize
                        }
                    }
                }
                line = readLine()
            }
        }

        return MPDResponse(
            status = MPDResponse.Status.OK,
            binaryResponse = binaryData,
            responseMap = responseMap
        )
    }
}
