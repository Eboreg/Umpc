package us.huseli.umpc.mpd.command

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import us.huseli.umpc.Constants.READ_BUFFER_SIZE
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDError
import us.huseli.umpc.mpd.response.MPDBaseResponse
import us.huseli.umpc.mpd.response.MPDBaseTextResponse
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.max
import kotlin.math.min

abstract class MPDBaseCommand<RT : MPDBaseResponse>(
    // val command: String,
    // val args: Collection<String> = emptyList(),
    val onFinish: ((RT) -> Unit)? = null,
) : LoggerInterface {
    private val readBuffer = ByteArray(READ_BUFFER_SIZE)
    private val lineBuffer = ByteArrayOutputStream()
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var readBufferReadPos = 0
    private var readBufferWritePos = 0

    private lateinit var inputStream: InputStream
    private lateinit var writer: PrintWriter

    val stringChannel = Channel<String?>()

    /**************************************************************************
     * ABSTRACT METHODS
     *************************************************************************/
    abstract suspend fun getResponse(socket: Socket): RT

    /**************************************************************************
     * PUBLIC METHODS
     *************************************************************************/
    suspend fun execute(socket: Socket): RT {
        log("START $this")
        val response = getResponse(socket)
        log("FINISH $this, response=$response", level = if (response.isSuccess) Log.INFO else Log.ERROR)
        return response
    }

    /**************************************************************************
     * PROTECTED METHODS
     *************************************************************************/
    private suspend fun streamLines() {
        var line: String?

        do {
            line = readLine()
            stringChannel.send(line)
        } while (line != null && line != "OK" && !line.startsWith("ACK "))

        stringChannel.close()
    }

    protected suspend fun <RT : MPDBaseTextResponse> fillTextResponse(command: String, response: RT): RT {
        var line: String?

        try {
            writeLine(command)
        } catch (e: Exception) {
            return response.finish(status = MPDBaseResponse.Status.ERROR_NET, exception = e)
        }

        while (true) {
            try {
                line = readLine()
            } catch (e: Exception) {
                return response.finish(status = MPDBaseResponse.Status.ERROR_NET, exception = e)
            }
            if (line != null) {
                if (line == "OK") {
                    return response.finish(status = MPDBaseResponse.Status.OK)
                } else if (line.startsWith("ACK ")) {
                    return response.finish(
                        status = MPDBaseResponse.Status.ERROR_MPD,
                        mpdError = MPDError.fromString(line),
                    )
                } else response.putLine(line)
            } else {
                return response.finish(status = MPDBaseResponse.Status.EMPTY_RESPONSE)
            }
        }
    }

    protected suspend fun readBinary(size: Int): ByteArray {
        val data = ByteArray(size)
        var dataRead = 0
        var dataToRead: Int
        var readyData: Int

        while (dataRead < size) {
            readyData = dataReady()
            dataToRead = min(readyData, size - dataRead)
            System.arraycopy(readBuffer, readBufferReadPos, data, dataRead, dataToRead)
            dataRead += dataToRead
            readBufferReadPos += dataToRead
            if (dataReady() == 0 && dataRead != size) fillReadBuffer()
        }

        skipByte()
        return data
    }

    protected suspend fun readLine(): String? {
        var localReadPos = readBufferReadPos
        lineBuffer.reset()

        while (true) {
            if (localReadPos == readBufferWritePos) {
                lineBuffer.write(readBuffer, readBufferReadPos, localReadPos - readBufferReadPos)
                fillReadBuffer()
                if (readBufferWritePos < 1) break
                localReadPos = 0
                continue
            }
            if (readBuffer[localReadPos] == '\n'.code.toByte()) {
                lineBuffer.write(readBuffer, readBufferReadPos, localReadPos - readBufferReadPos)
                readBufferReadPos = localReadPos + 1
                break
            }
            localReadPos++
        }
        return lineBuffer.toString().takeIf { it.isNotEmpty() }
    }

    protected suspend fun <RT : MPDBaseResponse> withSocket(socket: Socket, onFinish: suspend () -> RT): RT {
        return withContext(Dispatchers.IO) {
            inputStream = socket.getInputStream()
            writer = PrintWriter(socket.getOutputStream())
            onFinish()
        }
    }

    protected fun writeLine(line: String) {
        writer.println(line)
        writer.flush()
    }

    /**************************************************************************
     * PRIVATE METHODS
     *************************************************************************/
    private fun dataReady(): Int = readBufferWritePos - readBufferReadPos

    @Suppress("LiftReturnOrAssignment")
    private suspend fun fillReadBuffer() {
        withContext(Dispatchers.IO) {
            try {
                readBufferWritePos = max(inputStream.read(readBuffer, 0, READ_BUFFER_SIZE), 0)
            } catch (e: Exception) {
                log("fillReadBuffer: $e, cause=${e.cause}", Log.ERROR)
                readBufferWritePos = 0
                // throw MPDCommandException(e)
            }
            readBufferReadPos = 0
        }
    }

    private suspend fun skipByte() {
        var dataRead = 0
        var readyData: Int
        var dataToRead: Int

        while (dataRead < 1) {
            readyData = dataReady()
            dataToRead = min(readyData, 1 - dataRead)
            dataRead += dataToRead
            readBufferReadPos += dataToRead
            if (dataReady() == 0 && dataRead != 1) fillReadBuffer()
        }
    }
}
