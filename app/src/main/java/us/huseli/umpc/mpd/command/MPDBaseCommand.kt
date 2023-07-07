package us.huseli.umpc.mpd.command

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.umpc.Constants.READ_BUFFER_SIZE
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.mpd.MPDFilterContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.max
import kotlin.math.min

open class MPDCommandException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    constructor(cause: Throwable) : this(cause.toString(), cause)
}

class MPDCommandEmptyResponseException : MPDCommandException("Empty response")

abstract class MPDBaseCommand(
    val command: String,
    val args: Collection<String> = emptyList(),
    val onFinish: ((MPDResponse) -> Unit)? = null,
) : LoggerInterface {
    var retries = 0

    private val readBuffer = ByteArray(READ_BUFFER_SIZE)
    private val lineBuffer = ByteArrayOutputStream()

    private var readBufferReadPos = 0
    private var readBufferWritePos = 0

    private lateinit var inputStream: InputStream
    private lateinit var writer: PrintWriter

    protected abstract suspend fun getResponse(): MPDResponse

    suspend fun execute(socket: Socket): MPDResponse {
        retries++
        return withContext(Dispatchers.IO) {
            try {
                inputStream = socket.getInputStream()
                writer = PrintWriter(socket.getOutputStream())
                getResponse()
            } catch (e: MPDCommandException) {
                throw e
            } catch (e: Exception) {
                throw MPDCommandException(e)
            }
        }
    }

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

        skipBytes(1)
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
                try {
                    lineBuffer.write(readBuffer, readBufferReadPos, localReadPos - readBufferReadPos)
                    readBufferReadPos = localReadPos + 1
                    break
                } catch (e: Exception) {
                    log(
                        "MPDBaseCommand",
                        "readBufferReadPos=$readBufferReadPos, localReadPos=$localReadPos, readBufferWritePos=$readBufferWritePos, readBuffer.size=${readBuffer.size}",
                        Log.ERROR
                    )
                    throw e
                }
            }
            localReadPos++
        }
        return lineBuffer.toString().takeIf { it.isNotEmpty() }
    }

    @Suppress("SameParameterValue")
    private suspend fun skipBytes(size: Int) {
        var dataRead = 0
        var readyData: Int
        var dataToRead: Int

        while (dataRead < size) {
            readyData = dataReady()
            dataToRead = min(readyData, size - dataRead)
            dataRead += dataToRead
            readBufferReadPos += dataToRead
            if (dataReady() == 0 && dataRead != size) fillReadBuffer()
        }
    }

    @Suppress("RedundantSuspendModifier")
    protected suspend fun writeLine(line: String) {
        writer.println(line)
        writer.flush()
    }

    override fun toString() = "${javaClass.simpleName}[${getCommand(command, args)}]"

    override fun equals(other: Any?) = other is MPDBaseCommand && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    companion object {
        val responseRegex = Regex("^([^:]*): (.*)$")

        fun getCommand(command: String, args: Collection<String> = emptyList()) =
            if (args.isEmpty()) command
            else "$command ${args.joinToString(" ") { "\"${MPDFilterContext.escape(it)}\"" }}"

        fun parseResponseLine(line: String): Pair<String, String>? =
            responseRegex.find(line)?.groupValues?.let { if (it.size == 3) it[1] to it[2] else null }
    }
}
