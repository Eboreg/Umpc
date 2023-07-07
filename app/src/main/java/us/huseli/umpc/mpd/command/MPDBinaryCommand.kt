package us.huseli.umpc.mpd.command

import android.util.Log
import us.huseli.umpc.data.MPDResponse

class MPDBinaryCommand(
    command: String,
    args: Collection<String> = emptyList(),
    onFinish: ((MPDResponse) -> Unit)? = null,
) : MPDBaseCommand(command, args, onFinish) {
    override suspend fun getResponse(): MPDResponse {
        log("START $this")
        val response = getBinaryResponse()
        log("FINISH $this, returning $response", if (response.isSuccess) Log.INFO else Log.ERROR)
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
                    return MPDResponse(isSuccess = false, error = "No binary data returned")
                }
                if (line.startsWith("ACK ")) {
                    return MPDResponse(isSuccess = false, error = line.substring(4))
                }
            } else {
                throw MPDCommandEmptyResponseException()
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
                            return MPDResponse(isSuccess = false, error = "Got too much data")
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
            isSuccess = true,
            binaryResponse = binaryData,
            responseMap = responseMap
        )
    }
}
