package us.huseli.umpc.mpd

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDAudioFormat
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.data.plus
import us.huseli.umpc.data.queueDataStore
import us.huseli.umpc.data.sorted
import us.huseli.umpc.data.sortedByYear
import us.huseli.umpc.data.toMPDSong
import us.huseli.umpc.data.toMPDStatus
import us.huseli.umpc.data.toNative
import us.huseli.umpc.data.toProto
import us.huseli.umpc.mpd.client.MPDBinaryClient
import us.huseli.umpc.mpd.client.MPDClient
import us.huseli.umpc.mpd.client.MPDClientException
import us.huseli.umpc.mpd.client.MPDIdleClient
import us.huseli.umpc.mpd.engine.MPDControlEngine
import us.huseli.umpc.mpd.engine.MPDImageEngine
import us.huseli.umpc.mpd.engine.MPDPlaylistEngine
import us.huseli.umpc.mpd.engine.MessageEngine
import us.huseli.umpc.mpd.engine.SettingsEngine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class Engines(
    val settings: SettingsEngine,
    val image: MPDImageEngine,
    val control: MPDControlEngine,
    val message: MessageEngine,
    val playlist: MPDPlaylistEngine,
)

@Singleton
class MPDRepository @Inject constructor(
    private val ioScope: CoroutineScope,
    val streamPlayer: MPDStreamPlayer,
    @ApplicationContext private val context: Context,
) : LoggerInterface {
    private var statusJob: Job? = null
    private val onMPDChangeListeners = mutableListOf<OnMPDChangeListener>()

    // private val _consumeState = MutableStateFlow(ConsumeState.OFF)
    // private val _singleState = MutableStateFlow(SingleState.OFF)
    private val _currentSong = MutableStateFlow<MPDSong?>(null)
    private val _currentSongDuration = MutableStateFlow<Double?>(null)
    private val _currentSongElapsed = MutableStateFlow<Double?>(null)
    private val _currentSongId = MutableStateFlow<Int?>(null)
    private val _currentSongPosition = MutableStateFlow<Int?>(null)

    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _albumsWithSongs = MutableStateFlow<List<MPDAlbumWithSongs>>(listOf())
    private val _currentAudioFormat = MutableStateFlow<MPDAudioFormat?>(null)
    private val _currentBitrate = MutableStateFlow<Int?>(null)
    private val _outputs = MutableStateFlow<List<MPDOutput>>(emptyList())
    private val _playerState = MutableStateFlow(PlayerState.STOP)
    private val _queue = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _randomState = MutableStateFlow(false)
    private val _repeatState = MutableStateFlow(false)
    private val _volume = MutableStateFlow(100)

    val engines: Engines
    var client = MPDClient(ioScope)
    var binaryClient = MPDBinaryClient(ioScope)
    private var idleClient = MPDIdleClient(ioScope)

    val albumArtDirectory = File(context.cacheDir, "albumArt").apply { mkdirs() }
    val thumbnailDirectory = File(albumArtDirectory, "thumbnails").apply { mkdirs() }

    val currentSong = _currentSong.asStateFlow()
    val currentSongDuration = _currentSongDuration.asStateFlow()
    val currentSongElapsed = _currentSongElapsed.asStateFlow()
    val currentSongId = _currentSongId.asStateFlow()
    val currentSongPosition = _currentSongPosition.asStateFlow()
    val currentBitrate = _currentBitrate.asStateFlow()
    val currentAudioFormat = _currentAudioFormat.asStateFlow()

    val albums = _albums.asStateFlow()
    val outputs = _outputs.asStateFlow()
    val playerState = _playerState.asStateFlow()
    val queue = _queue.asStateFlow()
    val randomState = _randomState.asStateFlow()
    val repeatState = _repeatState.asStateFlow()
    val volume = _volume.asStateFlow()

    init {
        engines = Engines(
            settings = SettingsEngine(context),
            image = MPDImageEngine(this, ioScope),
            control = MPDControlEngine(this),
            message = MessageEngine(this),
            playlist = MPDPlaylistEngine(this, context, ioScope),
        )

        ioScope.launch {
            context.queueDataStore.data.collect {
                _queue.value = it.toNative()
            }
        }

        ioScope.launch {
            engines.settings.credentials.collect { credentials ->
                client.setCredentials(credentials)
                binaryClient.setCredentials(credentials)
                idleClient.setCredentials(credentials)

                try {
                    client.start()
                    binaryClient.start()

                    loadStatus()
                    loadQueue()
                    loadAlbums()
                    loadOutputs()
                    engines.playlist.loadStoredPlaylists()
                    watch()
                } catch (e: MPDClientException) {
                    engines.message.addError(e.message)
                    log(e.clientClass, e.message, Log.ERROR)
                }
            }
        }

        ioScope.launch {
            // Fetch (or empty) current song info as soon as it changes.
            _currentSongPosition.collect {
                if (engines.control.stopAfterCurrent.value) {
                    engines.control.stop()
                    engines.control.disableStopAfterCurrent()
                }
                if (it != null) loadCurrentSong()
                else _currentSong.value = null
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
                                loadStatus()
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

    fun fetchAlbumWithSongsListsByArtist(
        artist: String,
        onFinish: (List<MPDAlbumWithSongs>, List<MPDAlbumWithSongs>) -> Unit,
    ) {
        /**
         * Searches both artist and album artist tags.
         * Because we cannot do OR queries, we have to resort to this
         * inefficient bullshit in order to search both artist & albumartist.
         *
         * Songs lists returned by the "artist" filter will likely not be
         * complete album, but rather single tracks from compilations etc.
         * So, in addition, we need to fetch all songs from any such albums
         * separately.
         */
        // Albums where the artist is _not_ the album artist:
        var nonAlbumArtistAlbums = listOf<MPDAlbumWithSongs>()
        // Albums where the artist _is_ the album artist:
        var albumArtistAlbums = listOf<MPDAlbumWithSongs>()

        client.enqueueMultiMap(mpdFind { equals("artist", artist) }) { response ->
            // We got songs where this artist is in the "artist" tag:
            response.extractSongs().toMutableSet().also { songs ->
                nonAlbumArtistAlbums = songs.filter { it.artist != it.album.artist }.groupByAlbum()
                albumArtistAlbums = songs.filter { it.artist == it.album.artist }.groupByAlbum()
            }
            // The nonAlbumArtistAlbums are likely not complete (as they
            // contain songs by other artists), so the songs for those will
            // have to be fetched separately:
            fetchAlbumWithSongsListsByAlbumList(nonAlbumArtistAlbums.map { it.album }) { aws ->
                nonAlbumArtistAlbums = aws
                fetchSongListByAlbumArtist(artist) { songs ->
                    albumArtistAlbums = albumArtistAlbums.plus(songs.groupByAlbum())
                    onFinish(albumArtistAlbums.sortedByYear(), nonAlbumArtistAlbums.sortedByYear())
                }
            }
        }
    }

    /** In: 1 album. Out: All songs for this album. Updates _albumsWithSongs. */
    fun fetchSongListByAlbum(album: MPDAlbum, onFinish: (List<MPDSong>) -> Unit) =
        client.enqueueMultiMap(album.searchFilter.find()) { response ->
            val songs = response.extractSongs()
            onFinish(songs)
            _albumsWithSongs.value = _albumsWithSongs.value.plus(MPDAlbumWithSongs(album, songs))
        }

    /** Will also update this._albumsWithSongs. */
    private fun fetchSongListByAlbumArtist(albumArtist: String, onFinish: (List<MPDSong>) -> Unit) =
        client.enqueueMultiMap(mpdFind { equals("albumartist", albumArtist) }) { response ->
            response.extractSongs().also {
                onFinish(it)
                _albumsWithSongs.value = _albumsWithSongs.value.plus(it.groupByAlbum())
            }
        }

    fun getAlbumWithSongsByAlbum(album: MPDAlbum, onFinish: (MPDAlbumWithSongs) -> Unit) {
        val aws = _albumsWithSongs.value.find { it.album == album }

        if (aws != null) onFinish(aws)
        else fetchSongListByAlbum(album) {
            onFinish(MPDAlbumWithSongs(album, it))
        }
    }

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) = onMPDChangeListeners.add(listener)

    fun search(term: String, onFinish: (List<MPDSong>) -> Unit) {
        /**
         * MPD cannot combine search terms with logical OR for some reason, so
         * we cannot select a list of tags to search, but must use "any". As
         * this may give a lot of search results we don't want, additional
         * filtering must be applied.
         */
        if (term.isNotEmpty()) {
            client.enqueueMultiMap(mpdSearch { contains("any", term) }) { response ->
                onFinish(
                    response.extractSongs().filter {
                        it.album.name.contains(term, true) ||
                        it.artist.contains(term, true) ||
                        it.album.artist.contains(term, true) ||
                        it.title.contains(term, true)
                    }
                )
            }
        }
    }

    // fun setConsumeState(state: ConsumeState) = client.value?.enqueue("consume ${state.value}")
    // fun setSingleState(state: SingleState) = client.value?.enqueue("single ${state.value}")

    private fun fetchAlbumWithSongsListsByAlbumList(
        albums: List<MPDAlbum>,
        onFinish: (List<MPDAlbumWithSongs>) -> Unit,
    ) {
        /** Gets songs for albums in batch. Updates _albumsWithSongs. */
        val albumsWithSongs = mutableListOf<MPDAlbumWithSongs>()
        var i = 0

        if (albums.isEmpty()) onFinish(albumsWithSongs)

        albums.forEach { album ->
            client.enqueueMultiMap(album.searchFilter.find()) { response ->
                val albumWithSongs = response.extractSongs().groupByAlbum()[0]

                albumsWithSongs.add(albumWithSongs)
                _albumsWithSongs.value = _albumsWithSongs.value.plus(albumWithSongs)
                if (++i == albums.size) onFinish(albumsWithSongs)
            }
        }
    }

    /** Caches every album artist and their albums, no songs. */
    private fun loadAlbums() = client.enqueueMultiMap("list albumsort group albumartistsort") { response ->
        _albums.value = response.extractAlbums().sorted()
    }

    private fun loadCurrentSong() = client.enqueue("currentsong") { response ->
        _currentSong.value = response.responseMap.mapValues { it.value.first() }.toMPDSong()?.also { song ->
            song.duration?.let { _currentSongDuration.value = it }
        }
    }

    private fun loadOutputs(onFinish: ((List<MPDOutput>) -> Unit)? = null) =
        client.enqueueMultiMap("outputs") { response ->
            response.extractOutputs().also {
                _outputs.value = it
                onFinish?.invoke(it)
            }
        }

    private fun loadQueue() = client.enqueueMultiMap("playlistinfo") { response ->
        response.extractSongs().also { songs ->
            ioScope.launch {
                context.queueDataStore.updateData { currentQueue ->
                    currentQueue.toBuilder()
                        .clearSongs()
                        .addAllSongs(songs.toProto())
                        .build()
                }
            }
        }
    }

    private fun loadStatus() = client.enqueue("status") { response ->
        response.responseMap.mapValues { it.value.first() }.toMPDStatus()?.also { status ->
            status.volume?.let { _volume.value = it }
            status.repeat?.let { _repeatState.value = it }
            status.random?.let { _randomState.value = it }
            // status.single?.let { _singleState.value = it }
            // status.consume?.let { _consumeState.value = it }
            status.playerState?.let { _playerState.value = it }

            engines.message.addError(status.error)
            _currentSongElapsed.value = status.currentSongElapsed
            _currentSongDuration.value = status.currentSongDuration
            _currentSongId.value = status.currentSongId
            _currentBitrate.value = status.bitrate
            _currentSongPosition.value = status.currentSongPosition
            _currentAudioFormat.value = status.audioFormat
        }
    }

    // https://mpd.readthedocs.io/en/latest/protocol.html#querying-mpd-s-status
    private fun watch() = idleClient.start { response ->
        val subsystems = response.extractChanged()

        if (subsystems.contains("playlist")) loadQueue()
        if (subsystems.contains("output")) loadOutputs()
        if (subsystems.contains("player") || subsystems.contains("mixer") || subsystems.contains("options")) {
            loadStatus()
        }

        onMPDChangeListeners.forEach { listener -> listener.onMPDChanged(subsystems) }
    }
}
