package us.huseli.umpc

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.umpc.Constants.DYNAMIC_PLAYLIST_CHUNK_SIZE
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.dynamicPlaylistDataStore
import us.huseli.umpc.data.queueDataStore
import us.huseli.umpc.data.toNative
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.repository.DynamicPlaylistRepository
import kotlin.math.max
import kotlin.math.min

class DynamicPlaylistState(
    private val context: Context,
    private val playlist: DynamicPlaylist,
    private val repo: DynamicPlaylistRepository,
    private val ioScope: CoroutineScope,
    replaceCurrentQueue: Boolean,
    playOnLoad: Boolean,
    onLoaded: (() -> Unit)? = null,
) : OnMPDChangeListener, LoggerInterface {
    private val mutex = Mutex()
    private var songPositionListener: Job? = null
    private var currentOffset = 0

    init {
        log("DYNAMICPLAYLISTSTATE: init. currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
        repo.registerOnMPDChangeListener(this)

        ioScope.launch {
            mutex.withLock {
                if (shouldLoadSongsFromMPD(playlist)) {
                    log("DYNAMICPLAYLISTSTATE: should load songs from MPD. currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
                    loadSongsFromMPD { songs ->
                        ioScope.launch {
                            val songsToAdd = min(DYNAMIC_PLAYLIST_CHUNK_SIZE, songs.size)
                            saveSongsToDisk(songs)
                            fillMPDQueue(
                                filenames = songs.subList(0, songsToAdd).map { it.filename },
                                replaceCurrentQueue = replaceCurrentQueue,
                                playOnLoad = playOnLoad,
                                onFinish = onLoaded,
                            )
                            updateCurrentOffset(currentOffset + songsToAdd)
                        }
                    }
                } else {
                    currentOffset =
                        if (replaceCurrentQueue) 0
                        else min(loadCurrentOffsetFromDisk(), loadFilenamesCountFromDisk())
                    val length =
                        if (replaceCurrentQueue) DYNAMIC_PLAYLIST_CHUNK_SIZE
                        else max(DYNAMIC_PLAYLIST_CHUNK_SIZE - getQueue().songsCount, 0)

                    log("DYNAMICPLAYLISTSTATE: should load filenames from disk. length=$length, currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")

                    loadFilenamesFromDisk(currentOffset, length).also { filenames ->
                        val queueFilenames =
                            if (!replaceCurrentQueue) getQueue().songsList.map { it.filename }
                            else emptyList()
                        val filteredFilenames = filenames - queueFilenames.toSet()

                        log("DYNAMICPLAYLISTSTATE: loaded filenames from disk. length=$length, filenames.size=${filenames.size}, filteredFilenames.size=${filteredFilenames.size}, currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
                        fillMPDQueue(
                            filenames = filteredFilenames,
                            replaceCurrentQueue = replaceCurrentQueue,
                            playOnLoad = playOnLoad,
                            onFinish = onLoaded,
                        )
                        updateCurrentOffset(currentOffset + filenames.size)
                    }
                }
            }
        }

        songPositionListener = ioScope.launch {
            repo.currentSongPosition.filterNotNull().distinctUntilChanged().collect { position ->
                mutex.withLock {
                    val filesToAdd = (DYNAMIC_PLAYLIST_CHUNK_SIZE / 2) - getQueue().songsCount + position + 1
                    log("DYNAMICPLAYLISTSTATE: current song position changed. position=$position, queue size=${getQueue().songsCount}, filesToAdd=$filesToAdd, currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
                    if (filesToAdd > 0) {
                        loadFilenamesFromDisk(currentOffset, filesToAdd).also { filenames ->
                            fillMPDQueue(filenames = filenames)
                        }
                        updateCurrentOffset(currentOffset + filesToAdd)
                    }
                }
            }
        }
    }

    private suspend fun fillMPDQueue(
        filenames: List<String>,
        replaceCurrentQueue: Boolean = false,
        playOnLoad: Boolean = false,
        onFinish: (() -> Unit)? = null,
    ) {
        val firstPosition = if (replaceCurrentQueue) 0 else getQueue().songsCount + 1

        log("DYNAMICPLAYLISTSTATE: fill MPD queue. filenames.size=${filenames.size}, firstPosition=$firstPosition, currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
        if (replaceCurrentQueue) repo.clearQueue()
        repo.enqueueSongsLast(filenames) { response ->
            if (response.isSuccess && playOnLoad) repo.playSongByPosition(firstPosition)
            onFinish?.invoke()
        }
    }

    private suspend fun getQueue() = context.queueDataStore.data.first()

    private suspend fun loadCurrentOffsetFromDisk(): Int = context.dynamicPlaylistDataStore.data.first().currentOffset

    private suspend fun loadFilenamesCountFromDisk(): Int = context.dynamicPlaylistDataStore.data.first().filenamesCount

    private suspend fun loadFilenamesFromDisk(offset: Int, length: Int): List<String> =
        context.dynamicPlaylistDataStore.data.first().run {
            log(
                "DYNAMICPLAYLISTSTATE: loading filenames from disk. offset=$offset, length=$length, start=${
                    min(offset, filenamesCount)
                }, end=${min(offset + length, filenamesCount)}, currentOffset=$currentOffset"
            )
            filenamesList.filterNotNull().subList(
                min(offset, filenamesCount),
                min(offset + length, filenamesCount)
            )
        }

    private fun loadSongsFromMPD(onFinish: (List<MPDSong>) -> Unit) =
        repo.search(playlist.filter.mpdFilter) { response ->
            if (response.isSuccess) {
                val songs = response.extractSongs()
                onFinish(if (playlist.shuffle) songs.shuffled() else songs)
                repo.updateDynamicPlaylist(playlist, songCount = songs.size)
            }
        }

    private fun saveSongsToDisk(songs: List<MPDSong>) = ioScope.launch {
        playlist.toProto(filenames = songs.map { it.filename }, currentOffset = currentOffset)?.let {
            context.dynamicPlaylistDataStore.updateData { it }
        }
    }

    private suspend fun shouldLoadSongsFromMPD(playlist: DynamicPlaylist): Boolean =
        try {
            context.dynamicPlaylistDataStore.data.first().toNative() != playlist
        } catch (e: Exception) {
            true
        }

    private fun updateCurrentOffset(value: Int) {
        log("DYNAMICPLAYLISTSTATE: update current offset. value=$value, currentOffset=$currentOffset")
        currentOffset = value
        ioScope.launch {
            context.dynamicPlaylistDataStore.updateData { current ->
                current.toBuilder().setCurrentOffset(value).build()
            }
        }
    }

    suspend fun close() {
        songPositionListener?.cancelAndJoin()
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("database")) {
            loadSongsFromMPD { songs ->
                ioScope.launch {
                    saveSongsToDisk(songs)
                    val songsToAdd =
                        (DYNAMIC_PLAYLIST_CHUNK_SIZE / 2) -
                        getQueue().songsCount +
                        (repo.currentSongPosition.value ?: 0) + 1

                    if (songsToAdd > 0) {
                        fillMPDQueue(songs.subList(0, songsToAdd).map { it.filename })
                        updateCurrentOffset(songsToAdd)
                    }
                }
            }
        }
    }
}
