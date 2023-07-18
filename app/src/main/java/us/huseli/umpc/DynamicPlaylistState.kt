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
import us.huseli.umpc.data.dynamicPlaylistDataStore
import us.huseli.umpc.data.queueDataStore
import us.huseli.umpc.data.toNative
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import kotlin.math.max
import kotlin.math.min

class DynamicPlaylistState(
    private val context: Context,
    private val playlist: DynamicPlaylist,
    private val repo: MPDRepository,
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

        ioScope.launch {
            mutex.withLock {
                if (shouldLoadFilenamesFromPDB(playlist)) {
                    log("DYNAMICPLAYLISTSTATE: should load filenames from PDB. currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
                    loadFilenamesFromMPD { filenames ->
                        ioScope.launch {
                            val filesToAdd = min(DYNAMIC_PLAYLIST_CHUNK_SIZE, filenames.size)
                            saveFilenamesToDisk(filenames)
                            fillMPDQueue(
                                filenames = filenames.subList(0, filesToAdd),
                                replaceCurrentQueue = replaceCurrentQueue,
                                playOnLoad = playOnLoad,
                            )
                            updateCurrentOffset(currentOffset + filesToAdd)
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
                        )
                        updateCurrentOffset(currentOffset + filenames.size)
                    }
                }
            }
            onLoaded?.invoke()
        }

        repo.registerOnMPDChangeListener(this@DynamicPlaylistState)

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
    ) {
        var isPlayCalled = false
        val firstPosition = if (replaceCurrentQueue) 0 else getQueue().songsCount + 1

        log("DYNAMICPLAYLISTSTATE: fill MPD queue. filenames.size=${filenames.size}, firstPosition=$firstPosition, currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
        if (replaceCurrentQueue) repo.engines.control.clearQueue()

        filenames.forEach { filename ->
            log("DYNAMICPLAYLISTSTATE: will enqueue $filename. filenames.size=${filenames.size}, firstPosition=$firstPosition, currentOffset=$currentOffset, replaceCurrentQueue=$replaceCurrentQueue, playOnLoad=$playOnLoad")
            repo.engines.control.enqueueSongLast(filename) { response ->
                if (response.isSuccess && playOnLoad && !isPlayCalled) {
                    repo.engines.control.play(firstPosition)
                    isPlayCalled = true
                }
            }
        }
    }

    private suspend fun getQueue() = context.queueDataStore.data.first()

    private suspend fun loadCurrentOffsetFromDisk(): Int = context.dynamicPlaylistDataStore.data.first().currentOffset

    private suspend fun loadFilenamesCountFromDisk(): Int = context.dynamicPlaylistDataStore.data.first().filenamesCount

    private suspend fun loadFilenamesFromDisk(offset: Int, length: Int): List<String> =
        context.dynamicPlaylistDataStore.data.first().run {
            log("DYNAMICPLAYLISTSTATE: loading filenames from disk. offset=$offset, length=$length, start=${min(offset, filenamesCount)}, end=${min(offset + length, filenamesCount)}, currentOffset=$currentOffset")
            filenamesList.filterNotNull().subList(
                min(offset, filenamesCount),
                min(offset + length, filenamesCount)
            )
        }

    private fun loadFilenamesFromMPD(onFinish: (List<String>) -> Unit) =
        repo.client.enqueue(playlist.filter.mpdFilter.search()) { response ->
            if (response.isSuccess) {
                onFinish(if (playlist.shuffle) response.extractFilenames().shuffled() else response.extractFilenames())
            }
        }

    private fun saveFilenamesToDisk(filenames: List<String>) {
        ioScope.launch {
            context.dynamicPlaylistDataStore.updateData {
                playlist.toProto(filenames = filenames, currentOffset = currentOffset)
            }
        }
    }

    private suspend fun shouldLoadFilenamesFromPDB(playlist: DynamicPlaylist): Boolean =
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
            loadFilenamesFromMPD { filenames ->
                ioScope.launch {
                    saveFilenamesToDisk(filenames)
                    val filesToAdd =
                        (DYNAMIC_PLAYLIST_CHUNK_SIZE / 2) -
                        getQueue().songsCount +
                        (repo.currentSongPosition.value ?: 0) + 1

                    if (filesToAdd > 0) {
                        fillMPDQueue(filenames.subList(0, filesToAdd))
                        updateCurrentOffset(filesToAdd)
                    }
                }
            }
        }
    }
}
