package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDBaseResponse
import us.huseli.umpc.mpd.response.MPDBinaryResponse
import java.net.Socket

class MPDBinaryCommand(
    val command: String,
    val args: Collection<String> = emptyList(),
    onFinish: ((MPDBinaryResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDBinaryResponse>(onFinish) {
    override suspend fun getResponse(socket: Socket): MPDBinaryResponse {
        return try {
            withSocket(socket) { getBinaryResponse() }
        } catch (e: Exception) {
            MPDBinaryResponse().finish(
                status = MPDBaseResponse.Status.ERROR_NET,
                exception = e,
            )
        }
    }

    private suspend fun getBinaryResponse(): MPDBinaryResponse {
        var length = 0
        var dataToRead = 0
        var firstRun = true
        var line: String?
        val response = MPDBinaryResponse()

        // Outer loop makes server requests until all chunks are fetched:
        while (dataToRead > 0 || firstRun) {
            writeLine(getCommand(command, args.plus((length - dataToRead).toString())))
            line = readLine()

            if (line != null) {
                if (firstRun && line.startsWith("OK")) {
                    return response.finish(
                        status = MPDBaseResponse.Status.ERROR_OTHER,
                        error = "No binary data returned"
                    )
                }
                if (line.startsWith("ACK ")) {
                    return response.finish(
                        status = MPDBaseResponse.Status.ERROR_MPD,
                        error = line.substring(4)
                    )
                }
            } else return response.finish(status = MPDBaseResponse.Status.EMPTY_RESPONSE)

            // Inner loop handles individual server responses:
            while (line != null && !line.startsWith("OK")) {
                MPDBaseResponse.parseResponseLine(line)?.also { (key, value) ->
                    if (firstRun && key == "size") {
                        length = value.toInt()
                        dataToRead = length
                        firstRun = false
                        response.setLength(length)
                    }
                    if (key == "binary") {
                        val chunkSize = value.toInt()
                        val chunk = readBinary(chunkSize)

                        if (dataToRead - chunkSize < 0) {
                            return response.finish(
                                status = MPDBaseResponse.Status.ERROR_OTHER,
                                error = "Got too much data"
                            )
                        } else {
                            response.putBinaryChunk(chunk, length - dataToRead, chunkSize)
                            dataToRead -= chunkSize
                        }
                    }
                }
                line = readLine()
            }
        }
        return response.finish(status = MPDBaseResponse.Status.OK)
    }

    override fun equals(other: Any?) =
        other is MPDBinaryCommand && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    override fun toString() = "${javaClass.simpleName}[${getCommand(command, args)}]"
}
