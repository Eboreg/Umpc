package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.Constants.DYNAMIC_PLAYLIST_CHUNK_SIZE
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.mpdFilter
import java.io.Closeable
import java.io.File
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

    override fun toString() = "${key.displayName} ${comparator.displayName} \"$value\""
}

@Parcelize
data class DynamicPlaylist(
    val filter: DynamicPlaylistFilter,
    val shuffle: Boolean = false,
    val lastModified: Instant = Instant.now(),
) : Parcelable {
    override fun equals(other: Any?) = other is DynamicPlaylist && other.filter == filter
    override fun hashCode() = filter.hashCode()
    override fun toString() = filter.toString()
}

class DynamicPlaylistQueue(cacheDir: File) {
    private val queueFile = File(cacheDir, "dynamicPlaylist")
    private val tempFile = File("${queueFile.path}.tmp")
    private var buffer = emptyList<String>()
    private var linesInQueueFile = 0

    val size: Int
        get() = linesInQueueFile + buffer.size

    fun getFilenames(length: Int): List<String> {
        if (length > buffer.size && linesInQueueFile > 0) fillBuffer()
        val lines = buffer.subList(0, min(length, buffer.size))
        buffer = buffer.subList(lines.size, buffer.size)
        return lines
    }

    fun setFilenames(filenames: List<String>) {
        buffer = filenames.subList(0, min(BUFFER_SIZE, filenames.size))
        if (filenames.size > BUFFER_SIZE) {
            queueFile.printWriter().use { writer ->
                filenames.subList(BUFFER_SIZE, filenames.size).forEach {
                    writer.println(it)
                    linesInQueueFile++
                }
            }
        }
    }

    fun isEmpty(): Boolean = buffer.isEmpty() && linesInQueueFile <= 0

    private fun fillBuffer(): Int {
        /**
         * Removes the first max `length` lines from beginning of queue file
         * and adds them to this.buffer. Returns number of lines actually read.
         */
        var counter = 0
        val lines = mutableListOf<String>()

        queueFile.bufferedReader().use { reader ->
            tempFile.printWriter().use { writer ->
                do {
                    val line = reader.readLine()
                    if (line != null) {
                        if (counter < BUFFER_SIZE) {
                            lines.add(line)
                            counter++
                        } else writer.println(line)
                    }
                } while (line != null)
            }
        }
        if (counter > 0) {
            buffer = buffer.toMutableList().apply { addAll(lines) }
            tempFile.renameTo(queueFile)
        }
        linesInQueueFile -= counter
        return counter
    }

    companion object {
        const val BUFFER_SIZE = 200
    }
}

class DynamicPlaylistState(
    cacheDir: File,
    val playlist: DynamicPlaylist,
    val repo: MPDRepository,
    private val ioScope: CoroutineScope,
) : OnMPDChangeListener, Closeable {
    private var pastFilenames = emptyList<String>()
    private var songPositionListener: Job? = null
    private var queueListener: Job? = null
    private val queue = DynamicPlaylistQueue(cacheDir)

    init {
        getFileList {
            ioScope.launch {
                fillQueue()
            }
        }

        repo.registerOnMPDChangeListener(this@DynamicPlaylistState)

        songPositionListener = ioScope.launch {
            repo.currentSongPosition.filterNotNull().collect { position ->
                refillMPDQueue(mpdQueuePosition = position, mpdQueueSize = repo.queue.value.size)
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

    private fun refillMPDQueue(mpdQueuePosition: Int, mpdQueueSize: Int) {
        /**
         * If current queue position is over half of DYNAMIC_PLAYLIST_CHUNK_SIZE, fill up the queue so that this is no
         * longer the case.
         */
        if (mpdQueueSize - mpdQueuePosition <= DYNAMIC_PLAYLIST_CHUNK_SIZE / 2 && queue.size > 0) {
            val filesToAdd: List<String>
            val numberOfFilesToAdd = min(
                (DYNAMIC_PLAYLIST_CHUNK_SIZE / 2) - mpdQueueSize + mpdQueuePosition - 1,
                queue.size
            )
            filesToAdd = queue.getFilenames(numberOfFilesToAdd)
            pastFilenames = pastFilenames.toMutableList().apply { addAll(filesToAdd) }

            filesToAdd.forEach { filename -> repo.client.enqueue("add", filename) }
        }
    }

    private fun fillQueue() {
        val filenames = queue.getFilenames(DYNAMIC_PLAYLIST_CHUNK_SIZE)

        repo.client.enqueue("clear")
        pastFilenames = pastFilenames.toMutableList().apply { addAll(filenames) }

        filenames.forEach { filename -> repo.client.enqueue("add", filename) }
    }

    private fun getFileList(onFinish: (() -> Unit)? = null) {
        repo.client.enqueue(playlist.filter.mpdFilter.search()) { response ->
            if (response.isSuccess) {
                val filenames = response.extractFilenames().minus(pastFilenames.toSet())
                queue.setFilenames(if (playlist.shuffle) filenames.shuffled() else filenames)
            }
            onFinish?.invoke()
        }
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("database")) getFileList()
    }

    override fun close() {
        songPositionListener?.cancel()
        queueListener?.cancel()
    }
}
