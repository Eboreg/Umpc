package us.huseli.umpc.mpd.request

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.umpc.data.toMPDError
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.response.BaseMPDResponse
import us.huseli.umpc.mpd.response.MPDBinaryResponse
import java.io.InputStream
import java.io.PrintWriter
import java.net.Socket

class MPDBinaryRequest(
    command: String,
    val args: Collection<Any> = emptyList(),
    onFinish: ((MPDBinaryResponse) -> Unit)? = null,
) : BaseMPDRequest<MPDBinaryResponse>(command, onFinish) {
    override suspend fun getResponse(socket: Socket): MPDBinaryResponse = withContext(Dispatchers.IO) {
        getBinaryResponse(socket.getInputStream(), PrintWriter(socket.getOutputStream(), true))
    }

    private suspend fun getBinaryResponse(inputStream: InputStream, writer: PrintWriter): MPDBinaryResponse {
        var length = 0
        var dataToRead = 0
        var firstRun = true
        var line: String?
        val response = MPDBinaryResponse()

        // Outer loop makes server requests until all chunks are fetched:
        while (dataToRead > 0 || firstRun) {
            writer.println(formatMPDCommand(command, args.plus((length - dataToRead).toString())))
            line = readLine(inputStream)

            if (line != null) {
                if (firstRun && line.startsWith("OK")) {
                    return response.finish(
                        status = BaseMPDResponse.Status.ERROR_OTHER,
                        error = "No binary data returned"
                    )
                }
                if (line.startsWith("ACK ")) {
                    return response.finish(
                        status = BaseMPDResponse.Status.ERROR_MPD,
                        mpdError = line.toMPDError(),
                    )
                }
            } else return response.finish(status = BaseMPDResponse.Status.EMPTY_RESPONSE)

            // Inner loop handles individual server responses:
            while (line != null && !line.startsWith("OK")) {
                BaseMPDResponse.parseResponseLine(line)?.also { (key, value) ->
                    if (firstRun && key == "size") {
                        length = value.toInt()
                        dataToRead = length
                        firstRun = false
                        response.setLength(length)
                    }
                    if (key == "binary") {
                        val chunkSize = value.toInt()
                        val chunk = readBinary(inputStream, chunkSize)

                        if (dataToRead - chunkSize < 0) {
                            return response.finish(
                                status = BaseMPDResponse.Status.ERROR_OTHER,
                                error = "Got too much data"
                            )
                        } else {
                            response.putBinaryChunk(chunk, length - dataToRead, chunkSize)
                            dataToRead -= chunkSize
                        }
                    }
                }
                line = readLine(inputStream)
            }
        }
        return response.finish(status = BaseMPDResponse.Status.OK)
    }

    override fun equals(other: Any?) =
        other is MPDBinaryRequest && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    override fun toString() = "${javaClass.simpleName}[${formatMPDCommand(command, args)}]"
}
