package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDServer
import us.huseli.umpc.data.MPDServerCredentials
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.request.BaseMPDRequest
import us.huseli.umpc.mpd.request.MPDRequest
import us.huseli.umpc.mpd.response.BaseMPDResponse
import us.huseli.umpc.repository.SettingsRepository
import java.io.IOException
import java.net.Socket

abstract class BaseMPDClient(
    protected val ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) : LoggerInterface {
    enum class State { STARTED, PREPARED, READY, RUNNING, IO_ERROR, AUTH_ERROR }

    private val requestMutex = Mutex()
    private val requestQueue = MutableStateFlow<List<BaseMPDRequest<*>>>(emptyList())
    private val requestQueueMutex = Mutex()
    private lateinit var credentials: MPDServerCredentials
    private var retryJob: Job? = null

    protected val _connectedServer = MutableStateFlow<MPDServer?>(null)
    protected val _protocolVersion = MutableStateFlow<MPDVersion?>(null)
    protected val _state = MutableStateFlow(State.STARTED)
    protected val listeners = mutableListOf<MPDClientListener>()
    protected val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected var socket = Socket()
    protected var worker: Job? = null

    open val retryInterval = 3000L
    open val soTimeout = 5000

    val connectedServer = _connectedServer.asStateFlow()
    val protocolVersion = _protocolVersion.asStateFlow()
    val state = _state.asStateFlow()

    init {
        ioScope.launch {
            settingsRepository.currentServer.distinctUntilChanged().collect { server ->
                release()
                _state.value = State.PREPARED
                if (server != null) {
                    credentials = server
                    start()
                } else {
                    _connectedServer.value = null
                    _protocolVersion.value = null
                }
            }
        }
    }

    private suspend inline fun release() = withContext(Dispatchers.IO) {
        worker?.cancel()
        socket.close()
        requestQueueMutex.withLock { requestQueue.value = emptyList() }
        _state.value = State.STARTED
    }

    fun registerListener(listener: MPDClientListener) {
        listeners.add(listener)
    }

    fun <T : BaseMPDRequest<RT>, RT : BaseMPDResponse> enqueue(request: T, force: Boolean = false): T {
        ioScope.launch {
            requestQueueMutex.withLock {
                if (force || !requestQueue.value.contains(request)) {
                    log("ENQUEUE $request, requestQueue=${requestQueue.value}")
                    requestQueue.value += request
                } else {
                    requestQueue.value
                        .filterIsInstance(request.javaClass)
                        .find { it == request }
                        ?.addCallbacks(request.onFinishCallbacks)
                }
            }
        }
        return request
    }

    protected open suspend fun start() {
        catchError {
            worker?.cancel()
            worker = workerScope.launch {
                while (isActive) {
                    when (_state.value) {
                        State.PREPARED -> connect()
                        State.READY -> {
                            val request = requestQueueMutex.withLock {
                                requestQueue.value.firstOrNull()?.also { requestQueue.value -= it }
                            }
                            if (request != null) runRequest(request)
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
                socket = Socket(credentials.hostname, credentials.port)

                val reader = socket.getInputStream().bufferedReader()
                val responseLine = reader.readLine()

                if (responseLine == null || !responseLine.startsWith("OK MPD"))
                    throw MPDClientException("Expected OK MPD response, got: $responseLine")

                MPDVersion(responseLine.substringAfter("OK MPD ")).also {
                    _connectedServer.value = MPDServer(credentials.hostname, credentials.port, it)
                    _protocolVersion.value = it
                }

                credentials.password?.also { password ->
                    val response = MPDRequest(formatMPDCommand("password", password)).execute(socket)
                    if (!response.isSuccess) throw MPDAuthException("Error on password request: ${response.error}")
                }

                MPDRequest("tagtypes").execute(socket).extractValues("tagtype").also { tagTypes ->
                    val command = formatMPDCommand(
                        "tagtypes disable",
                        tagTypes.filter { !wantedTagTypes.contains(it.lowercase()) },
                    )
                    MPDRequest(command).execute(socket)
                }

                socket.soTimeout = soTimeout
                _state.value = State.READY
            }
        }
    }

    protected inline fun <T : Any> catchError(request: BaseMPDRequest<*>? = null, inner: () -> T?): T? = try {
        inner()
    } catch (e: Exception) {
        if (e is IOException || e is MPDException) {
            if (e is MPDAuthException) _state.value = State.AUTH_ERROR
            else {
                _state.value = State.IO_ERROR
                startRetryLoop()
            }
            _connectedServer.value = null
            _protocolVersion.value = null
        }
        if (e !is CancellationException)
            listeners.forEach { it.onMPDClientError(this, e, request) }
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

    private suspend inline fun <RT : BaseMPDResponse> runRequest(request: BaseMPDRequest<RT>) {
        catchError(request) {
            try {
                _state.value = State.RUNNING
                val response = requestMutex.withLock { request.execute(socket) }
                if (response.status == BaseMPDResponse.Status.EMPTY_RESPONSE) {
                    _state.value = State.PREPARED
                    enqueue(request, force = true)
                } else {
                    _state.value = State.READY
                    request.runCallbacks(response)
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
