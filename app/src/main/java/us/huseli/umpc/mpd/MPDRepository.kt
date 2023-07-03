package us.huseli.umpc.mpd

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.filterByAlbum
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.data.sortedByYear
import us.huseli.umpc.data.toMPDSong
import us.huseli.umpc.data.toMPDStatus
import us.huseli.umpc.mpd.client.MPDBinaryClient
import us.huseli.umpc.mpd.client.MPDClient
import us.huseli.umpc.mpd.client.MPDIdleClient
import us.huseli.umpc.mpd.engine.MPDControlEngine
import us.huseli.umpc.mpd.engine.MPDImageEngine
import us.huseli.umpc.mpd.engine.MessageEngine
import us.huseli.umpc.mpd.engine.SettingsEngine
import javax.inject.Inject
import javax.inject.Singleton

class Engines(
    val settings: SettingsEngine,
    val image: MPDImageEngine,
    val control: MPDControlEngine,
    val message: MessageEngine,
)

@Singleton
class MPDRepository @Inject constructor(
    private val ioScope: CoroutineScope,
    @ApplicationContext context: Context,
) {
    val engines: Engines
    private var statusJob: Job? = null

    private val _client = MutableStateFlow<MPDClient?>(null)
    private val _binaryClient = MutableStateFlow<MPDBinaryClient?>(null)
    private val _idleClient = MutableStateFlow<MPDIdleClient?>(null)

    // private val _consumeState = MutableStateFlow(ConsumeState.OFF)
    // private val _singleState = MutableStateFlow(SingleState.OFF)
    private val _currentSong = MutableStateFlow<MPDSong?>(null)
    private val _currentSongAlbumArt = MutableStateFlow<MPDAlbumArt?>(null)
    private val _currentSongDuration = MutableStateFlow<Double?>(null)
    private val _currentSongElapsed = MutableStateFlow<Double?>(null)
    private val _currentSongId = MutableStateFlow<Int?>(null)
    private val _currentSongIndex = MutableStateFlow<Int?>(null)

    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _currentBitrate = MutableStateFlow<Int?>(null)
    private val _outputs = MutableStateFlow<List<MPDOutput>>(emptyList())
    private val _playerState = MutableStateFlow(PlayerState.STOP)
    private val _queue = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _randomState = MutableStateFlow(false)
    private val _repeatState = MutableStateFlow(false)
    private val _songs = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _volume = MutableStateFlow(100)

    // val consumeState = _consumeState.asStateFlow()
    // val currentBitrate = _currentBitrate.asStateFlow()
    // val singleState = _singleState.asStateFlow()
    val binaryClient = _binaryClient.asStateFlow()
    val client = _client.asStateFlow()

    val currentSong = _currentSong.asStateFlow()
    val currentSongAlbumArt = _currentSongAlbumArt.asStateFlow()
    val currentSongDuration = _currentSongDuration.asStateFlow()
    val currentSongElapsed = _currentSongElapsed.asStateFlow()
    val currentSongId = _currentSongId.asStateFlow()
    val currentSongIndex = _currentSongIndex.asStateFlow()

    val albums = _albums.asStateFlow()
    val outputs = _outputs.asStateFlow()
    val playerState = _playerState.asStateFlow()
    val queue = _queue.asStateFlow()
    val randomState = _randomState.asStateFlow()
    val repeatState = _repeatState.asStateFlow()
    val songs = _songs.asStateFlow()
    val volume = _volume.asStateFlow()

    init {
        engines = Engines(
            settings = SettingsEngine(context),
            image = MPDImageEngine(context, this, ioScope),
            control = MPDControlEngine(this),
            message = MessageEngine(this),
        )

        ioScope.launch {
            engines.settings.credentials.collect { credentials ->
                _client.value?.close()
                _binaryClient.value?.close()
                _idleClient.value?.close()

                try {
                    _client.value = MPDClient(ioScope, credentials).apply { initialize() }
                    _binaryClient.value = MPDBinaryClient(ioScope, credentials).apply { initialize() }
                    _idleClient.value = MPDIdleClient(ioScope, credentials).apply { initialize() }
                } catch (e: Exception) {
                    engines.message.setError(e.toString())
                }

                fetchStatus()
                fetchQueue()
                fetchAlbums()
                fetchOutputs()
                watch()
            }
        }

        ioScope.launch {
            // Fetch current song info as soon as it changes.
            _currentSongIndex.filterNotNull().distinctUntilChanged().collect {
                fetchCurrentSong()
            }
        }

        ioScope.launch {
            // While playing, query for status every 10 seconds.
            _playerState.collect { playerState ->
                if (playerState == PlayerState.PLAY) {
                    if (statusJob == null) {
                        statusJob = ioScope.launch {
                            while (isActive) {
                                delay(10_000)
                                fetchStatus()
                            }
                        }
                    }
                } else {
                    statusJob?.cancel()
                    statusJob = null
                }
            }
        }
    }

    fun fetchAlbumsWithSongsByAlbumArtist(artist: String, onFinish: (List<MPDAlbumWithSongs>) -> Unit) {
        fetchSongsByAlbumArtist(artist) { songs ->
            onFinish(songs.groupByAlbum().sortedByYear())
        }
    }

    fun fetchOutputs(onFinish: ((List<MPDOutput>) -> Unit)? = null) {
        _client.value?.enqueue("outputs") { response ->
            response.extractOutputs().also {
                _outputs.value = it
                onFinish?.invoke(it)
            }
        }
    }

    fun fetchSongs(artist: String, album: String, onFinish: ((List<MPDSong>) -> Unit)? = null) {
        val command = find { and(equals("album", album), equals("albumartist", artist)) }

        _client.value?.enqueue(command) { response ->
            val songs = response.extractSongs()
            onFinish?.invoke(songs)
            addSongs(songs)
        }
    }

    fun fetchSongsByArtist(artist: String, onFinish: (List<MPDSong>) -> Unit) {
        /**
         * Searches both artist and album artist tags.
         * Because we cannot do OR queries, we have to resort to this
         * inefficient bullshit in order to search both artist & albumartist.
         */
        _client.value?.enqueue(find { equals("artist", artist) }) { response ->
            val songs = response.extractSongs().toMutableSet()

            addSongs(songs)
            fetchSongsByAlbumArtist(artist) {
                songs.addAll(it)
                onFinish(songs.toList())
            }
        }
    }

    fun getAlbumWithSongsFlow(album: MPDAlbum): StateFlow<MPDAlbumWithSongs> =
        MutableStateFlow(MPDAlbumWithSongs(album, _songs.value.filterByAlbum(album))).apply {
            if (value.songs.isEmpty()) {
                fetchSongs(album.artist, album.name) {
                    value = value.copy(songs = it)
                }
            }
        }.asStateFlow()

    fun search(term: String, onFinish: (List<MPDSong>) -> Unit) {
        /**
         * MPD cannot combine search terms with logical OR for some reason, so
         * we cannot select a list of tags to search, but must use "any". As
         * this may give a lot of search results we don't want, additional
         * filtering must be applied.
         */
        if (term.isNotEmpty()) {
            _client.value?.enqueue(search { contains("any", term) }) { response ->
                onFinish(
                    response.extractSongs().filter {
                        it.album.contains(term, true) ||
                        it.artist.contains(term, true) ||
                        it.albumArtist.contains(term, true) ||
                        it.title.contains(term, true)
                    }
                )
            }
        }
    }

    // fun setConsumeState(state: ConsumeState) = client.value?.enqueue("consume ${state.value}")
    // fun setSingleState(state: SingleState) = client.value?.enqueue("single ${state.value}")

    /** PRIVATE METHODS ******************************************************/

    private fun addSongs(songs: Collection<MPDSong>) {
        if (songs.isNotEmpty()) {
            _songs.value = _songs.value.toMutableList().apply {
                // Looks weird, but thanks to MPDSong's custom equals()
                // method, we're actually replacing any old versions of
                // songs with new ones.
                removeAll(songs.toSet())
                addAll(songs)
            }
        }
    }

    private fun fetchAlbums() {
        _client.value?.enqueue("list albumsort group albumartistsort") { response ->
            response.extractAlbums().also { albums ->
                _albums.value = albums.sortedBy { it.name.lowercase() }
            }
        }
    }

    private fun fetchCurrentSong() {
        _client.value?.enqueue("currentsong") { response ->
            _currentSong.value = response.responseMap.toMPDSong()?.also { song ->
                song.duration?.let { _currentSongDuration.value = it }
                addSongs(listOf(song))
            }
        }
    }

    private fun fetchQueue() {
        _client.value?.enqueue("playlistinfo") { response ->
            response.extractSongs().also { songs ->
                _queue.value = songs
                addSongs(songs)
            }
        }
    }

    private fun fetchSongsByAlbumArtist(albumArtist: String, onFinish: (List<MPDSong>) -> Unit) {
        _client.value?.enqueue(find { equals("albumartist", albumArtist) }) { response ->
            response.extractSongs().also {
                onFinish(it)
                addSongs(it)
            }
        }
    }

    private fun fetchStatus() {
        _client.value?.enqueue("status") { response ->
            response.responseMap.toMPDStatus()?.also { status ->
                status.volume?.let { _volume.value = it }
                status.repeat?.let { _repeatState.value = it }
                status.random?.let { _randomState.value = it }
                // status.single?.let { _singleState.value = it }
                // status.consume?.let { _consumeState.value = it }
                status.playerState?.let { _playerState.value = it }

                engines.message.setError(status.error)
                _currentSongElapsed.value = status.currentSongElapsed
                _currentSongDuration.value = status.currentSongDuration
                _currentSongId.value = status.currentSongId
                _currentBitrate.value = status.bitrate
                _currentSongIndex.value = status.currentSongIndex
            }
        }
    }

    // https://mpd.readthedocs.io/en/latest/protocol.html#querying-mpd-s-status
    private fun watch() {
        ioScope.launch {
            _idleClient.filterNotNull().collect { client ->
                client.start { response ->
                    val changed = response.extractChanged()

                    if (changed.contains("playlist")) fetchQueue()
                    if (changed.contains("output")) fetchOutputs()
                    if (changed.contains("player") || changed.contains("mixer") || changed.contains("options")) {
                        fetchStatus()
                    }
                }
            }
        }
    }
}
