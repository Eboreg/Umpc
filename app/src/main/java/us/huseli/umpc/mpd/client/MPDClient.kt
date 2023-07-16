package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDCredentials
import us.huseli.umpc.mpd.command.MPDBaseCommand
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.command.MPDMultiMapCommand
import us.huseli.umpc.mpd.response.MPDBaseResponse
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.mpd.response.MPDMultiMapResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class MPDClientException(
    client: MPDClient,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    val clientClass: String = client.javaClass.simpleName
}

open class MPDClient(private val ioScope: CoroutineScope) : LoggerInterface {
    enum class State { STARTED, PREPARED, READY, RUNNING, ERROR }

    private val commandQueue = MutableSharedFlow<MPDBaseCommand<*>>(replay = 20)
    private val commandMutex = Mutex()
    private var credentials: MPDCredentials? = null
    private val wantedTagTypes = listOf(
        "Artist",
        "ArtistSort",
        "Album",
        "AlbumSort",
        "AlbumArtist",
        "AlbumArtistSort",
        "Title",
        "Track",
        "Date",
        "Disc",
    )

    protected val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected val state = MutableStateFlow(State.STARTED)
    protected var socket = Socket()

    fun enqueue(command: String, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        enqueue(command, listOf(), onFinish)

    fun enqueue(command: String, arg: String, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        enqueue(command, listOf(arg), onFinish)

    open fun enqueue(
        command: String,
        args: Collection<String> = emptyList(),
        onFinish: ((MPDMapResponse) -> Unit)? = null,
    ) = enqueue(MPDMapCommand(command, args, onFinish))

    fun <RT : MPDBaseResponse> enqueue(command: MPDBaseCommand<RT>) {
        ioScope.launch {
            log("ENQUEUE $command, commandQueue.replayCache=${commandQueue.replayCache}")
            commandQueue.emit(command)
        }
    }

    fun enqueueMultiMap(command: String, onFinish: ((MPDMultiMapResponse) -> Unit)? = null) =
        enqueue(MPDMultiMapCommand(command, onFinish = onFinish))

    fun enqueueMultiMap(command: String, arg: String, onFinish: ((MPDMultiMapResponse) -> Unit)? = null) =
        enqueue(MPDMultiMapCommand(command, args = listOf(arg), onFinish = onFinish))

    fun setCredentials(credentials: MPDCredentials) {
        if (credentials != this.credentials) {
            this.credentials = credentials
            state.value = State.PREPARED
        }
    }

    suspend fun start() {
        workerScope.launch {
            while (isActive) {
                when (state.value) {
                    State.PREPARED -> connect(failSilently = true)
                    State.READY -> commandQueue.collect { command -> runCommand(command) }
                    else -> {}
                }
                delay(100)
            }
        }
    }

    protected open suspend fun connect(failSilently: Boolean = false): Boolean {
        state.value = State.PREPARED
        return withContext(Dispatchers.IO) {
            try {
                socket.close()
                credentials?.run { socket = Socket(hostname, port) }

                val inputStream = socket.getInputStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val responseLine = reader.readLine()

                if (responseLine == null || !responseLine.startsWith("OK MPD")) {
                    throw MPDClientException(this@MPDClient, "Expected OK MPD response, got: $responseLine")
                }

                credentials?.password?.also { password ->
                    val response = MPDMapCommand("password", password).execute(socket)
                    if (!response.isSuccess) {
                        throw MPDClientException(this@MPDClient, "Error on password command: ${response.error}")
                    }
                }

                MPDMapCommand("tagtypes").execute(socket).extractTagTypes().also {
                    MPDMapCommand("tagtypes disable", it.minus(wantedTagTypes.toSet())).execute(socket)
                }

                state.value = State.READY
                true
            } catch (e: Exception) {
                state.value = State.ERROR
                if (!failSilently) {
                    if (e is MPDClientException) throw e
                    else throw MPDClientException(this@MPDClient, e.toString(), e)
                }
                false
            }
        }
    }

    private suspend fun <RT : MPDBaseResponse> runCommand(command: MPDBaseCommand<RT>) {
        try {
            state.value = State.RUNNING
            val response = commandMutex.withLock { command.execute(socket) }
            if (response.status == MPDBaseResponse.Status.EMPTY_RESPONSE) {
                connect(failSilently = true)
                enqueue(command)
            } else {
                state.value = State.READY
                command.onFinish?.invoke(response)
            }
        } catch (_: NullPointerException) {
            state.value = State.READY
        }
    }
}
