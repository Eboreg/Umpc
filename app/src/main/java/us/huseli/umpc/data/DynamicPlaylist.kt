package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.Constants.DYNAMIC_PLAYLIST_CHUNK_SIZE
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.mpdFilter
import java.io.Closeable
import java.time.Instant
import kotlin.math.min

enum class DynamicPlaylistFilterKey(val displayName: String, val mpdTag: String) {
    ARTIST("Artist", "artist"),
    ALBUM_ARTIST("Album artist", "albumartist"),
    ALBUM("Album", "album"),
    SONG_TITLE("Song title", "title"),
    FILENAME("File path", "file"),
}

enum class DynamicPlaylistFilterComparator(val displayName: String) {
    EQUALS("equals"),
    NOT_EQUALS("does not equal"),
    CONTAINS("contains"),
    NOT_CONTAINS("does not contain"),
}

@Parcelize
data class DynamicPlaylistFilter(
    val key: DynamicPlaylistFilterKey = DynamicPlaylistFilterKey.ARTIST,
    val value: String = "",
    val comparator: DynamicPlaylistFilterComparator = DynamicPlaylistFilterComparator.EQUALS,
) : Parcelable {
    @IgnoredOnParcel
    val mpdFilter: MPDFilter
        get() = mpdFilter {
            when (comparator) {
                DynamicPlaylistFilterComparator.EQUALS -> equals(key.mpdTag, value)
                DynamicPlaylistFilterComparator.NOT_EQUALS -> notEquals(key.mpdTag, value)
                DynamicPlaylistFilterComparator.CONTAINS -> contains(key.mpdTag, value)
                DynamicPlaylistFilterComparator.NOT_CONTAINS -> contains(key.mpdTag, value).not()
            }
        }
}

@Parcelize
data class DynamicPlaylist(
    val name: String,
    val filter: DynamicPlaylistFilter,
    val shuffle: Boolean = false,
    val lastModified: Instant = Instant.now(),
) : Parcelable {
    override fun equals(other: Any?) = other is DynamicPlaylist && other.name == name
    override fun hashCode() = name.hashCode()
}

class DynamicPlaylistState(
    val playlist: DynamicPlaylist,
    val repo: MPDRepository,
    private val ioScope: CoroutineScope
) : OnMPDChangeListener, Closeable {
    private val pastFiles = mutableListOf<String>()
    private val futureFiles = mutableListOf<String>()
    private var songPositionListener: Job? = null
    private var queueListener: Job? = null
    private val mutex = Mutex()

    init {
        getFiles {
            ioScope.launch {
                fillQueue()
            }
        }

        repo.registerOnMPDChangeListener(this@DynamicPlaylistState)

        songPositionListener = ioScope.launch {
            repo.currentSongPosition.filterNotNull().collect { position ->
                refillQueue(currentPosition = position, currentQueueSize = repo.queue.value.size)
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

    private suspend fun refillQueue(currentPosition: Int, currentQueueSize: Int) {
        /**
         * If current queue position is over half of DYNAMIC_PLAYLIST_CHUNK_SIZE, fill up the queue so that this is no
         * longer the case.
         */
        val futureFilesCount = mutex.withLock { futureFiles.size }

        if (currentQueueSize - currentPosition <= DYNAMIC_PLAYLIST_CHUNK_SIZE / 2 && futureFilesCount > 0) {
            val filesToAdd: List<String>

            mutex.withLock {
                val numberOfFilesToAdd = min(
                    (DYNAMIC_PLAYLIST_CHUNK_SIZE / 2) - currentQueueSize + currentPosition + 1,
                    futureFiles.size
                )
                filesToAdd = futureFiles.subList(0, numberOfFilesToAdd)
                pastFiles.addAll(filesToAdd)
            }

            filesToAdd.forEach { filename ->
                repo.client.enqueue("add", filename) { response ->
                    if (response.isSuccess) ioScope.launch {
                        mutex.withLock {
                            pastFiles.add(filename)
                            futureFiles.remove(filename)
                        }
                    }
                }
            }
        }
    }

    private suspend fun fillQueue() {
        val filenames: List<String> = mutex.withLock {
            futureFiles.subList(0, min(DYNAMIC_PLAYLIST_CHUNK_SIZE, futureFiles.size))
        }

        repo.client.enqueue("clear")

        filenames.forEach { filename ->
            repo.client.enqueue("add", filename) { response ->
                if (response.isSuccess) {
                    ioScope.launch {
                        mutex.withLock {
                            pastFiles.add(filename)
                            futureFiles.remove(filename)
                        }
                    }
                }
            }
        }
    }

    private fun getFiles(onFinish: (() -> Unit)? = null) {
        repo.client.enqueue(playlist.filter.mpdFilter.search()) { response ->
            if (response.isSuccess) {
                ioScope.launch {
                    mutex.withLock {
                        val filenames = response.extractFilenames().minus(pastFiles)
                        futureFiles.clear()
                        if (playlist.shuffle)
                            futureFiles.addAll(filenames.shuffled())
                        else
                            futureFiles.addAll(filenames)
                    }
                    onFinish?.invoke()
                }
            }
        }
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("database")) getFiles()
    }

    override fun close() {
        songPositionListener?.cancel()
        queueListener?.cancel()
    }
}
