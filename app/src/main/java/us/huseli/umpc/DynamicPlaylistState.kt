package us.huseli.umpc

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants.DYNAMIC_PLAYLIST_CHUNK_SIZE
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.dynamicPlaylistDataStore
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import java.io.Closeable
import kotlin.math.min

class DynamicPlaylistState(
    private val context: Context,
    private val playlist: DynamicPlaylist,
    private val repo: MPDRepository,
    private val ioScope: CoroutineScope,
    private val playOnLoad: Boolean,
    private val onLoaded: (() -> Unit)? = null,
) : OnMPDChangeListener, Closeable {
    private var pastFilenames = emptyList<String>()
    private var songPositionListener: Job? = null
    private var queueListener: Job? = null
    private var currentOffset = 0

    init {
        getMPDFileList { fillMPDQueueAndPlay() }

        repo.registerOnMPDChangeListener(this@DynamicPlaylistState)

        songPositionListener = ioScope.launch {
            repo.currentSongPosition.filterNotNull().collect { position ->
                val filesToAdd = (DYNAMIC_PLAYLIST_CHUNK_SIZE / 2) - repo.queue.value.size + position + 1
                if (filesToAdd > 0) refillMPDQueue(currentOffset, filesToAdd)
            }
        }
        /*
        queueListener = ioScope.launch {
            repo.queue.collect { queue ->
                repo.currentSongPosition.value?.let { position ->
                    refillQueue(currentPosition = position, currentQueueSize = queue.size)
                }
            }
        }
         */
    }

    private suspend fun getFilenames(offset: Int, length: Int): List<String> {
        return context.dynamicPlaylistDataStore.data.first().let {
            currentOffset = min(offset + length, it.filenamesCount)
            it.filenamesList.filterNotNull().subList(min(offset, it.filenamesCount), currentOffset)
        }
    }

    private fun setFilenames(filenames: List<String>, onFinish: () -> Unit) {
        ioScope.launch {
            context.dynamicPlaylistDataStore.updateData { current ->
                current.toBuilder()
                    .clearFilenames()
                    .addAllFilenames(filenames)
                    .build()
            }
            currentOffset = 0
            onFinish()
        }
    }

    private fun fillMPDQueueAndPlay() {
        ioScope.launch {
            var isPlayCalled = false

            getFilenames(0, DYNAMIC_PLAYLIST_CHUNK_SIZE).also { filenames ->
                repo.engines.control.clearQueue()
                pastFilenames = pastFilenames.toMutableList().apply { addAll(filenames) }
                filenames.forEach { filename ->
                    repo.engines.control.enqueueSongLast(filename) { response ->
                        if (response.isSuccess && !isPlayCalled) {
                            if (playOnLoad) repo.engines.control.play(0)
                            isPlayCalled = true
                        }
                    }
                }
                onLoaded?.invoke()
            }
        }
    }

    private fun getMPDFileList(onFinish: (() -> Unit)? = null) {
        repo.client.enqueue(playlist.filter.mpdFilter.search()) { response ->
            if (response.isSuccess) {
                val filenames = response.extractFilenames().minus(pastFilenames.toSet())
                setFilenames(if (playlist.shuffle) filenames.shuffled() else filenames) { onFinish?.invoke() }
            }
        }
    }

    private suspend fun refillMPDQueue(offset: Int, length: Int) {
        getFilenames(offset, length).also { filenames ->
            pastFilenames = pastFilenames.toMutableList().apply { addAll(filenames) }
            filenames.forEach { repo.engines.control.enqueueSongLast(it) }
        }
    }

    override fun close() {
        songPositionListener?.cancel()
        queueListener?.cancel()
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("database")) getMPDFileList()
    }
}
