package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDServer
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.command.BaseMPDCommand
import us.huseli.umpc.mpd.command.MPDCommand
import us.huseli.umpc.mpd.response.BaseMPDResponse
import us.huseli.umpc.repository.SettingsRepository
import java.io.IOException
import java.net.Socket

abstract class BaseMPDClient(
    protected val ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) : LoggerInterface {
    enum class State { STARTED, PREPARED, READY, RUNNING, IO_ERROR, AUTH_ERROR }

    private val commandMutex = Mutex()
    private val commandQueue = MutableStateFlow<List<BaseMPDCommand<*>>>(emptyList())
    private val commandQueueMutex = Mutex()
    private val credentials = settingsRepository.credentials
    private var retryJob: Job? = null

    protected val _connectedServer = MutableStateFlow<MPDServer?>(null)
    protected val _state = MutableStateFlow(State.STARTED)
    protected val listeners = mutableListOf<MPDClientListener>()
    protected val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected var socket = Socket()
    protected var worker: Job? = null

    open val retryInterval = 3000L
    open val soTimeout = 5000

    val connectedServer = _connectedServer.asStateFlow()
    val state = _state.asStateFlow()

    init {
        ioScope.launch {
            credentials.collect {
                release()
                _state.value = State.PREPARED
                start()
            }
        }
    }

    private suspend fun release() = withContext(Dispatchers.IO) {
        // worker?.cancelAndJoin()
        worker?.cancel()
        socket.close()
        commandQueueMutex.withLock { commandQueue.value = emptyList() }
        _state.value = State.STARTED
    }

    fun registerListener(listener: MPDClientListener) {
        listeners.add(listener)
    }

    protected fun <RT : BaseMPDResponse> enqueue(command: BaseMPDCommand<RT>, force: Boolean = false) {
        ioScope.launch {
            commandQueueMutex.withLock {
                if (force || !commandQueue.value.contains(command)) {
                    log("ENQUEUE $command, commandQueue=${commandQueue.value}")
                    commandQueue.value += command
                } else {
                    commandQueue.value
                        .filterIsInstance(command.javaClass)
                        .find { it == command }
                        ?.addCallbacks(command.onFinishCallbacks)
                }
            }
        }
    }

    protected open suspend fun start() {
        catchError {
            worker?.cancel()
            worker = workerScope.launch {
                while (isActive) {
                    when (_state.value) {
                        State.PREPARED -> connect()
                        State.READY -> {
                            val command = commandQueueMutex.withLock {
                                commandQueue.value.firstOrNull()?.also { commandQueue.value -= it }
                            }
                            if (command != null) runCommand(command)
                        }
                        else -> {}
                    }
                    delay(100)
                }
            }
        }
    }

    protected open suspend fun connect() {
        withContext(Dispatchers.IO) {
            catchError {
                socket.close()
                socket = Socket(credentials.value.hostname, credentials.value.port)

                val reader = socket.getInputStream().bufferedReader()
                val responseLine = reader.readLine()

                if (responseLine == null || !responseLine.startsWith("OK MPD"))
                    throw MPDClientException("Expected OK MPD response, got: $responseLine")

                MPDVersion(responseLine.substringAfter("OK MPD ")).also {
                    _connectedServer.value = MPDServer(credentials.value.hostname, credentials.value.port, it)
                }

                credentials.value.password?.also { password ->
                    val response = MPDCommand(formatMPDCommand("password", password)).execute(socket)
                    if (!response.isSuccess) throw MPDAuthException("Error on password command: ${response.error}")
                }

                MPDCommand("tagtypes").execute(socket).extractValues("tagtype").also { tagTypes ->
                    val command = formatMPDCommand(
                        "tagtypes disable",
                        tagTypes.filter { !wantedTagTypes.contains(it.lowercase()) },
                    )
                    MPDCommand(command).execute(socket)
                }

                socket.soTimeout = soTimeout
                _state.value = State.READY
            }
        }
    }

    protected inline fun <T : Any> catchError(command: BaseMPDCommand<*>? = null, inner: () -> T?): T? = try {
        inner()
    } catch (e: Exception) {
        if (e is IOException || e is MPDException) {
            if (e is MPDAuthException) _state.value = State.AUTH_ERROR
            else {
                _state.value = State.IO_ERROR
                startRetryLoop()
            }
            _connectedServer.value = null
        }
        if (e !is CancellationException)
            listeners.forEach { it.onMPDClientError(this, e, command) }
        null
    }

    protected fun startRetryLoop() {
        if (retryJob?.isActive != true) {
            retryJob = ioScope.launch {
                while (state.value == State.IO_ERROR) {
                    delay(retryInterval)
                    connect()
                }
            }
        }
    }

    private suspend fun <RT : BaseMPDResponse> runCommand(command: BaseMPDCommand<RT>) {
        catchError(command) {
            try {
                _state.value = State.RUNNING
                val response = commandMutex.withLock { command.execute(socket) }
                if (response.status == BaseMPDResponse.Status.EMPTY_RESPONSE) {
                    _state.value = State.PREPARED
                    enqueue(command, force = true)
                } else {
                    _state.value = State.READY
                    command.runCallbacks(response)
                }
            } catch (e: NullPointerException) {
                // When does this happen?
                _state.value = State.READY
            }
        }
    }

    companion object {
        private val wantedTagTypes = listOf(
            "album",
            "albumartist",
            "albumartistsort",
            "albumsort",
            "artist",
            "artistsort",
            "date",
            "disc",
            "title",
            "track",
        )
    }
}
