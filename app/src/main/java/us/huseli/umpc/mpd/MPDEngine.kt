package us.huseli.umpc.mpd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants
import us.huseli.umpc.ConsumeState
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.PlayerState
import us.huseli.umpc.SettingsRepository
import us.huseli.umpc.SingleState
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.filterByAlbum
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.data.sortedByYear
import us.huseli.umpc.data.toMPDSong
import us.huseli.umpc.data.toMPDStatus
import us.huseli.umpc.toBitmap
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class MPDEngine @Inject constructor(
    @ApplicationContext context: Context,
    private val ioScope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
) {
    private val albumArtDir = File(context.cacheDir, "albumArt").apply { mkdirs() }
    private val thumbnailDir = File(albumArtDir, "thumbnails").apply { mkdirs() }
    private var statusJob: Job? = null

    private val client = MutableStateFlow<MPDClient?>(null)
    private val binaryClient = MutableStateFlow<MPDBinaryClient?>(null)
    private val idleClient = MutableStateFlow<MPDIdleClient?>(null)

    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    // private val _consumeState = MutableStateFlow(ConsumeState.OFF)
    private val _currentBitrate = MutableStateFlow<Int?>(null)
    private val _currentSong = MutableStateFlow<MPDSong?>(null)
    private val _currentSongAlbumArt = MutableStateFlow<ImageBitmap?>(null)
    private val _currentSongDuration = MutableStateFlow<Double?>(null)
    private val _currentSongElapsed = MutableStateFlow<Double?>(null)
    private val _currentSongId = MutableStateFlow<Int?>(null)
    private val _currentSongIndex = MutableStateFlow<Int?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _fetchedAlbumArtKeys = mutableListOf<AlbumArtKey>()
    private val _outputs = MutableStateFlow<List<MPDOutput>>(emptyList())
    private val _playerState = MutableStateFlow(PlayerState.STOP)
    private val _queue = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _randomState = MutableStateFlow(false)
    private val _repeatState = MutableStateFlow(false)
    // private val _singleState = MutableStateFlow(SingleState.OFF)
    private val _songs = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _volume = MutableStateFlow(100)

    private val _currentAlbumArtKey = _currentSong.map { song ->
        song?.let { AlbumArtKey(it.albumArtist, it.album, it.filename) }
    }

    val albums = _albums.asStateFlow()
    // val consumeState = _consumeState.asStateFlow()
    // val currentBitrate = _currentBitrate.asStateFlow()
    val currentSong = _currentSong.asStateFlow()
    val currentSongAlbumArt = _currentSongAlbumArt.asStateFlow()
    val currentSongDuration = _currentSongDuration.asStateFlow()
    val currentSongElapsed = _currentSongElapsed.asStateFlow()
    val currentSongId = _currentSongId.asStateFlow()
    val currentSongIndex = _currentSongIndex.asStateFlow()
    val error = _error.asStateFlow()
    val outputs = _outputs.asStateFlow()
    val playerState = _playerState.asStateFlow()
    val queue = _queue.asStateFlow()
    val randomState = _randomState.asStateFlow()
    val repeatState = _repeatState.asStateFlow()
    // val singleState = _singleState.asStateFlow()
    // val songs = _songs.asStateFlow()
    val volume = _volume.asStateFlow()

    init {
        ioScope.launch {
            settingsRepository.credentials.collect { credentials ->
                client.value?.close()
                binaryClient.value?.close()
                idleClient.value?.close()

                client.value = MPDClient(ioScope, credentials).apply { initialize() }
                binaryClient.value = MPDBinaryClient(ioScope, credentials).apply { initialize() }
                idleClient.value = MPDIdleClient(ioScope, credentials).apply { initialize() }

                fetchStatus()
                fetchQueue()
                fetchAlbums()
                fetchOutputs()
                watch()
            }
        }

        ioScope.launch {
            _currentSongIndex.filterNotNull().distinctUntilChanged().collect {
                fetchCurrentSong()
            }
        }

        ioScope.launch {
            _currentAlbumArtKey.filterNotNull().distinctUntilChanged().collect { key ->
                _currentSongAlbumArt.value = null
                getAlbumArt(key, ImageRequestType.FULL) {
                    _currentSongAlbumArt.value = it.fullImage
                }
            }
        }

        ioScope.launch {
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

    fun enqueueLast(album: MPDAlbum, onFinish: (MPDResponse) -> Unit) {
        val command = findAdd { and(equals("albumartist", album.artist), equals("album", album.name)) }
        client.value?.enqueue(command, onFinish = onFinish)
    }

    fun enqueueLast(song: MPDSong, onFinish: (MPDResponse) -> Unit) {
        client.value?.enqueue("add", listOf(song.filename), onFinish)
    }

    fun enqueueNextAndPlay(song: MPDSong) {
        client.value?.enqueue("add", listOf(song.filename, "+0")) { response ->
            if (response.isSuccess) next()
        }
    }

    fun enqueueNextAndPlay(album: MPDAlbum) {
        val command = findAdd("+0") {
            and(equals("albumartist", album.artist), equals("album", album.name))
        }
        client.value?.enqueue(command) { response -> if (response.isSuccess) next() }
    }

    fun fetchOutputs(onFinish: ((List<MPDOutput>) -> Unit)? = null) {
        client.value?.enqueue("outputs") { response ->
            response.extractOutputs().also {
                _outputs.value = it
                onFinish?.invoke(it)
            }
        }
    }

    fun fetchSongs(artist: String, album: String, onFinish: ((List<MPDSong>) -> Unit)? = null) {
        val command = find { and(equals("album", album), equals("albumartist", artist)) }

        client.value?.enqueue(command) { response ->
            val songs = response.extractSongs()
            onFinish?.invoke(songs)
            addSongs(songs)
        }
    }

    fun fetchSongsByArtist(artist: String, onFinish: (List<MPDSong>) -> Unit) {
        /**
         * Searches both artist and album artist tags.
         * Because we cannot do OR queries, we have to resort to this
         * inefficient mess in order to search both artist & albumartist.
         */
        client.value?.enqueue(find { equals("artist", artist) }) { response ->
            val songs = response.extractSongs().toMutableSet()

            addSongs(songs)
            fetchSongsByAlbumArtist(artist) {
                songs.addAll(it)
                onFinish(songs.toList())
            }
        }
    }

    suspend fun getAlbumArt(
        key: AlbumArtKey,
        requestType: ImageRequestType,
        callback: (MPDAlbumArt) -> Unit
    ) {
        val fullImage: Bitmap? = File(albumArtDir, key.imageFilename).toBitmap()
        var thumbnail: Bitmap? = null

        if (requestType == ImageRequestType.THUMBNAIL || requestType == ImageRequestType.BOTH)
            thumbnail = File(thumbnailDir, key.imageFilename).toBitmap()

        if (fullImage != null && requestType == ImageRequestType.FULL)
            callback(MPDAlbumArt(key, fullImage = fullImage.asImageBitmap()))
        else if (thumbnail != null && fullImage != null)
            callback(MPDAlbumArt(key, fullImage = fullImage.asImageBitmap(), thumbnail = thumbnail.asImageBitmap()))
        else if (!_fetchedAlbumArtKeys.contains(key)) {
            if (key.filename == null) {
                // First we must have the filename of a song. Then try again.
                // Perhaps a song already exists in _songs?
                val song = _songs.value.filterByAlbum(key.albumArtist, key.album).firstOrNull()
                if (song != null) {
                    ioScope.launch {
                        getAlbumArt(key.copy(filename = song.filename), requestType, callback)
                    }
                } else {
                    // Otherwise, fetch songs.
                    fetchSongs(key.albumArtist, key.album) { songs ->
                        songs.firstOrNull()?.let { song ->
                            ioScope.launch {
                                getAlbumArt(key.copy(filename = song.filename), requestType, callback)
                            }
                        }
                    }
                }
            } else {
                // We have a song filename, now we can fetch albumart:
                binaryClient.value?.enqueue("albumart", listOf(key.filename)) { response ->
                    _fetchedAlbumArtKeys.add(key)
                    if (response.isSuccess) {
                        ioScope.launch {
                            saveAlbumArt(
                                data = response.binaryResponse,
                                imageFilename = key.imageFilename,
                                callback = { fullImage, thumbnail ->
                                    callback(MPDAlbumArt(key, fullImage.asImageBitmap(), thumbnail.asImageBitmap()))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    fun getAlbumsWithSongsByAlbumArtist(artist: String, onFinish: (List<MPDAlbumWithSongs>) -> Unit) {
        fetchSongsByAlbumArtist(artist) { songs ->
            onFinish(songs.groupByAlbum().sortedByYear())
        }
    }

    fun getAlbumWithSongs(album: MPDAlbum): StateFlow<MPDAlbumWithSongs> =
        MutableStateFlow(MPDAlbumWithSongs(album, _songs.value.filterByAlbum(album))).apply {
            if (value.songs.isEmpty()) {
                Log.i("MPDEngine", "getAlbumWithSongs: songs empty, running fetchSongs. album=$album")
                fetchSongs(album.artist, album.name) {
                    Log.i("MPDEngine", "getAlbumWithSongs: back from fetchSongs. album=$album, songs=$it")
                    value = value.copy(songs = it)
                }
            }
        }.asStateFlow()

    fun next() = client.value?.enqueue("next")

    fun play(songPosition: Int? = null) =
        client.value?.enqueue("play", listOf((songPosition ?: _currentSongIndex.value ?: 0).toString()))

    fun playOrPause() {
        when (_playerState.value) {
            PlayerState.PLAY -> pause()
            PlayerState.STOP -> play()
            PlayerState.PAUSE -> resume()
        }
    }

    fun playSongId(songId: Int) = client.value?.enqueue("playid $songId")

    fun previous() = client.value?.enqueue("previous")

    fun clearError() {
        _error.value = null
        client.value?.enqueue("clearerror")
    }

    fun search(term: String, onFinish: (List<MPDSong>) -> Unit) {
        /**
         * MPD cannot combine search terms with logical OR for some reason, so
         * we cannot select a list of tags to search, but must use "any". As
         * this may give a lot of search results we don't want, additional
         * filtering must be applied.
         */
        if (term.isNotEmpty()) {
            client.value?.enqueue(search { contains("any", term) }) { response ->
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

    fun seek(time: Double, songPosition: Int? = null) =
        if (songPosition != null) client.value?.enqueue("seek $songPosition $time")
        else client.value?.enqueue("seekcur $time")

    // fun setConsumeState(state: ConsumeState) = client.value?.enqueue("consume ${state.value}")

    // fun setSingleState(state: SingleState) = client.value?.enqueue("single ${state.value}")

    fun setOutputEnabled(id: Int, isEnabled: Boolean) =
        client.value?.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id")

    fun setVolume(@IntRange(0, 100) value: Int) =
        client.value?.enqueue("setvol $value")

    fun stop() = client.value?.enqueue("stop")

    fun toggleRandomState() =
        client.value?.enqueue("random ${if (_randomState.value) "0" else "1"}")

    fun toggleRepeatState() =
        client.value?.enqueue("repeat ${if (_repeatState.value) "0" else "1"}")

    /** PRIVATE METHODS ******************************************************/

    @Suppress("RedundantSuspendModifier")
    private suspend fun createThumbnail(fullImage: Bitmap, imageFilename: String): Bitmap {
        val factor = Constants.THUMBNAIL_SIZE.toFloat() / max(fullImage.width, fullImage.height)
        val width = (fullImage.width * factor).roundToInt()
        val height = (fullImage.height * factor).roundToInt()
        val file = File(thumbnailDir, imageFilename)
        val thumbnail = fullImage.scale(width, height)

        file.outputStream().use { outputStream ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        return thumbnail
    }

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
        client.value?.enqueue("list albumsort group albumartistsort") { response ->
            response.extractAlbums().also { albums ->
                _albums.value = albums.sortedBy { it.name.lowercase() }
            }
        }
    }

    private fun fetchCurrentSong() {
        client.value?.enqueue("currentsong") { response ->
            _currentSong.value = response.responseMap.toMPDSong()?.also { song ->
                song.duration?.let { _currentSongDuration.value = it }
                addSongs(listOf(song))
            }
        }
    }

    private fun fetchQueue() {
        client.value?.enqueue("playlistinfo") { response ->
            response.extractSongs().also { songs ->
                _queue.value = songs
                addSongs(songs)
            }
        }
    }

    private fun fetchSongsByAlbumArtist(albumArtist: String, onFinish: (List<MPDSong>) -> Unit) {
        client.value?.enqueue(find { equals("albumartist", albumArtist) }) { response ->
            response.extractSongs().also {
                onFinish(it)
                addSongs(it)
            }
        }
    }

    private fun fetchStatus() {
        client.value?.enqueue("status") { response ->
            response.responseMap.toMPDStatus()?.also { status ->
                Log.i("MPDEngine", "fetchStatus: $status")
                status.volume?.let { _volume.value = it }
                status.repeat?.let { _repeatState.value = it }
                status.random?.let { _randomState.value = it }
                // status.single?.let { _singleState.value = it }
                // status.consume?.let { _consumeState.value = it }
                status.playerState?.let { _playerState.value = it }

                _error.value = status.error
                _currentSongElapsed.value = status.currentSongElapsed
                _currentSongDuration.value = status.currentSongDuration
                _currentBitrate.value = status.bitrate
                _currentSongIndex.value = status.currentSongIndex
            }
        }
    }

    private fun pause() = client.value?.enqueue("pause 1")

    private fun resume() = client.value?.enqueue("pause 0")

    private suspend fun saveAlbumArt(
        data: ByteArray,
        imageFilename: String,
        callback: (Bitmap, Bitmap) -> Unit,
    ) {
        BitmapFactory.decodeByteArray(data, 0, data.size)?.also { fullImage ->
            val fullImageFile = File(albumArtDir, imageFilename)
            val factor = Constants.ALBUM_ART_MAXSIZE.toFloat() / max(fullImage.width, fullImage.height)
            val thumbnail = createThumbnail(fullImage, imageFilename)

            // Scale down full image if it happens to be very large:
            if (factor < 0) {
                val width = (fullImage.width * factor).roundToInt()
                val height = (fullImage.height * factor).roundToInt()
                val scaledFullImage = fullImage.scale(width, height)

                callback(scaledFullImage, thumbnail)
                fullImageFile.outputStream().use { outputStream ->
                    scaledFullImage.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                }
            } else {
                callback(fullImage, thumbnail)
                fullImageFile.outputStream().use { outputStream -> outputStream.write(data) }
            }
        }
    }

    // https://mpd.readthedocs.io/en/latest/protocol.html#querying-mpd-s-status
    private fun watch() {
        ioScope.launch {
            idleClient.filterNotNull().collect { client ->
                client.start { response ->
                    val changed = response.extractChanged()

                    if (changed.contains("playlist")) fetchQueue()
                    if (changed.contains("output")) fetchOutputs()
                    if (changed.contains("player") || changed.contains("mixer") || changed.contains("options"))
                        fetchStatus()
                }
            }
        }
    }
}
