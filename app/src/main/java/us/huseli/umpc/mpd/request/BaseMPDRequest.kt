package us.huseli.umpc.mpd.request

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import us.huseli.umpc.Constants.READ_BUFFER_SIZE
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.mpd.response.BaseMPDResponse
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Socket
import kotlin.math.max
import kotlin.math.min

abstract class BaseMPDRequest<T : BaseMPDResponse>(
    protected var command: String,
    onFinish: ((T) -> Unit)? = null
) : LoggerInterface {
    enum class Status { PENDING, RUNNING, FINISHED }

    private val _onFinishCallbacks = mutableListOf<(T) -> Unit>()
    private var _status = Status.PENDING
    private val readBuffer = ByteArray(READ_BUFFER_SIZE)
    private val lineBuffer = ByteArrayOutputStream()
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val stringChannel = Channel<String?>()

    private var readBufferReadPos = 0
    private var readBufferWritePos = 0

    val status: Status
        get() = _status
    val onFinishCallbacks: List<(T) -> Unit>
        get() = _onFinishCallbacks

    init {
        if (onFinish != null) _onFinishCallbacks.add(onFinish)
    }

    /**************************************************************************
     * ABSTRACT METHODS
     *************************************************************************/
    abstract suspend fun getResponse(socket: Socket): T

    /**************************************************************************
     * PUBLIC METHODS
     *************************************************************************/
    fun addCallback(callback: (T) -> Unit) = _onFinishCallbacks.add(callback)

    fun addCallbacks(callbacks: List<(T) -> Unit>) = _onFinishCallbacks.addAll(callbacks)

    suspend fun execute(socket: Socket): T {
        _status = Status.RUNNING
        log("START $this")
        return getResponse(socket).also {
            _status = Status.FINISHED
            log("FINISH $this, response=$it", level = it.logLevel)
        }
    }

    fun runCallbacks(response: T) = _onFinishCallbacks.forEach { it(response) }

    /**************************************************************************
     * PROTECTED METHODS
     *************************************************************************/
    protected suspend fun readBinary(inputStream: InputStream, size: Int): ByteArray {
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
            if (dataReady() == 0 && dataRead != size) fillReadBuffer(inputStream)
        }

        skipByte(inputStream)
        return data
    }

    protected suspend fun readLine(inputStream: InputStream): String? {
        var localReadPos = readBufferReadPos
        lineBuffer.reset()

        // TODO: What if localReadPos becomes larger?
        while (true) {
            if (localReadPos > readBuffer.size) {
                break
            }
            if (localReadPos == readBufferWritePos) {
                lineBuffer.write(readBuffer, readBufferReadPos, localReadPos - readBufferReadPos)
                fillReadBuffer(inputStream)
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

    /**************************************************************************
     * PRIVATE METHODS
     *************************************************************************/
    private fun dataReady(): Int = readBufferWritePos - readBufferReadPos

    private suspend fun fillReadBuffer(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            try {
                readBufferWritePos = max(inputStream.read(readBuffer, 0, READ_BUFFER_SIZE), 0)
            } catch (e: Exception) {
                log("fillReadBuffer: $e, cause=${e.cause}", Log.ERROR)
                readBufferWritePos = 0
                throw e
            }
            readBufferReadPos = 0
        }
    }

    private suspend fun skipByte(inputStream: InputStream) {
        var dataRead = 0
        var readyData: Int
        var dataToRead: Int

        while (dataRead < 1) {
            readyData = dataReady()
            dataToRead = min(readyData, 1 - dataRead)
            dataRead += dataToRead
            readBufferReadPos += dataToRead
            if (dataReady() == 0 && dataRead != 1) fillReadBuffer(inputStream)
        }
    }

    private suspend fun streamLines(inputStream: InputStream) {
        var line: String?

        do {
            line = readLine(inputStream)
            stringChannel.send(line)
        } while (line != null && line != "OK" && !line.startsWith("ACK "))

        stringChannel.close()
    }
}
